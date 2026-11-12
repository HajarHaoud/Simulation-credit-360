package ma.credit360.service;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ma.credit360.entity.Dossier;
import ma.credit360.entity.PieceDeposee;
import ma.credit360.entity.PieceRequise;
import ma.credit360.entity.Statut;
import ma.credit360.entity.Utilisateur;
import ma.credit360.repository.PieceDeposeeRepository;
import ma.credit360.repository.PieceRequiseRepository;
import ma.credit360.repository.UtilisateurRepository;
import ma.credit360.web.dto.ChecklistDto;
import ma.credit360.web.dto.DossierDto;
import ma.credit360.web.dto.PieceDto;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PieceService {

    public record Telechargement(Resource ressource, String nom, String typeMime) {
    }

    private final DossierService dossierService;
    private final PieceRequiseRepository requises;
    private final PieceDeposeeRepository deposees;
    private final StockageService stockage;
    private final UtilisateurRepository utilisateurs;

    public PieceService(
            DossierService dossierService,
            PieceRequiseRepository requises,
            PieceDeposeeRepository deposees,
            StockageService stockage,
            UtilisateurRepository utilisateurs) {

        this.dossierService = dossierService;
        this.requises = requises;
        this.deposees = deposees;
        this.stockage = stockage;
        this.utilisateurs = utilisateurs;
    }

    /**
     * Consulte la checklist des pièces d'un dossier.
     */
    @Transactional(readOnly = true)
    public ChecklistDto checklist(Utilisateur u, String reference) {

        Dossier d = dossierService.trouverAccessible(u, reference);

        return construire(d);
    }

    /**
     * Dépose un fichier ou remplace le fichier existant.
     */
    @Transactional
    public ChecklistDto deposer(
            Utilisateur u,
            String reference,
            Long pieceId,
            MultipartFile fichier) {

        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Veuillez sélectionner un fichier."
            );
        }

        Dossier d = dossierService.trouverAccessible(u, reference);

        verifierModifiable(d);

        PieceRequise requise = requises
                .findByIdAndTypeCreditId(pieceId, d.getTypeCredit().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pièce inconnue pour ce type de crédit."
                ));

        StockageService.Stocke stocke;

        try {
            stocke = stockage.enregistrer(fichier);
        } catch (UncheckedIOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'enregistrement du fichier."
            );
        }

        // Si la transaction SQL échoue, le nouveau fichier sera supprimé.
        supprimerNouveauFichierSiRollback(stocke.nomStockage());

        PieceDeposee depot = deposees
                .findByDossierIdAndPieceRequiseId(d.getId(), requise.getId())
                .orElse(null);

        String ancienFichier = null;

        Utilisateur auteur = utilisateurs.getReferenceById(u.getId());

        String nom = nomAffiche(fichier.getOriginalFilename());

        if (depot == null) {

            depot = new PieceDeposee(
                    d,
                    requise,
                    nom,
                    stocke.typeContenu(),
                    stocke.taille(),
                    stocke.nomStockage(),
                    LocalDateTime.now(),
                    auteur
            );

        } else {

            ancienFichier = depot.getNomStockage();

            depot.remplacer(
                    nom,
                    stocke.typeContenu(),
                    stocke.taille(),
                    stocke.nomStockage(),
                    LocalDateTime.now(),
                    auteur
            );
        }

        deposees.save(depot);

        d.setDateMaj(LocalDate.now());

        // L'ancien fichier ne sera supprimé qu'après le commit SQL.
        if (ancienFichier != null) {
            supprimerApresCommit(ancienFichier);
        }

        return construire(d);
    }

    /**
     * Supprime un fichier déposé.
     */
    @Transactional
    public ChecklistDto supprimer(
            Utilisateur u,
            String reference,
            Long pieceId) {

        Dossier d = dossierService.trouverAccessible(u, reference);

        verifierModifiable(d);

        PieceDeposee depot = deposees
                .findByDossierIdAndPieceRequiseId(d.getId(), pieceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun fichier déposé pour cette pièce."
                ));

        String ancienFichier = depot.getNomStockage();

        deposees.delete(depot);

        d.setDateMaj(LocalDate.now());

        // Suppression physique uniquement après validation SQL.
        supprimerApresCommit(ancienFichier);

        return construire(d);
    }

    /**
     * Télécharge un fichier déposé.
     */
    @Transactional(readOnly = true)
    public Telechargement fichier(
            Utilisateur u,
            String reference,
            Long pieceId) {

        Dossier d = dossierService.trouverAccessible(u, reference);

        PieceDeposee depot = deposees
                .findByDossierIdAndPieceRequiseId(d.getId(), pieceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun fichier déposé pour cette pièce."
                ));

        var chemin = stockage.chemin(depot.getNomStockage());

        if (!Files.isRegularFile(chemin)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Fichier introuvable sur le serveur."
            );
        }

        return new Telechargement(
                new FileSystemResource(chemin),
                depot.getNomFichier(),
                depot.getTypeContenu()
        );
    }

    /**
     * Soumet un dossier à l'analyse si toutes les pièces obligatoires sont présentes.
     */
    @Transactional
    public DossierDto soumettre(Utilisateur u, String reference) {

        Dossier d = dossierService.trouverAccessible(u, reference);

        if (d.getStatut() != Statut.A_COMPLETER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seuls les dossiers « À compléter » peuvent être soumis à l'analyse."
            );
        }

        ChecklistDto checklist = construire(d);

        if (!checklist.complete()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pièces manquantes : "
                            + String.join(", ", checklist.manquantes()) + "."
            );
        }

        d.setStatut(Statut.EN_ANALYSE);
        d.setDateMaj(LocalDate.now());

        return DossierDto.de(d);
    }

    /**
     * Construit la checklist à partir des pièces requises
     * et des fichiers déjà déposés.
     */
    private ChecklistDto construire(Dossier d) {

        List<PieceRequise> liste = requises
                .findByTypeCreditIdOrderByOrdreAsc(d.getTypeCredit().getId());

        Map<Long, PieceDeposee> parPiece = deposees
                .findByDossierId(d.getId())
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getPieceRequise().getId(),
                        Function.identity()
                ));

        List<PieceDto> pieces = liste.stream()
                .map(r -> PieceDto.de(r, parPiece.get(r.getId())))
                .toList();

        int requisesObligatoires = (int) pieces.stream()
                .filter(PieceDto::obligatoire)
                .count();

        List<String> manquantes = pieces.stream()
                .filter(p -> p.obligatoire() && p.fichier() == null)
                .map(PieceDto::libelle)
                .toList();

        int deposeesObligatoires =
                requisesObligatoires - manquantes.size();

        return new ChecklistDto(
                pieces,
                deposeesObligatoires,
                requisesObligatoires,
                manquantes.isEmpty(),
                manquantes,
                d.getStatut() == Statut.A_COMPLETER
        );
    }

    /**
     * Vérifie que le dossier accepte encore des modifications de pièces.
     */
    private void verifierModifiable(Dossier d) {

        if (d.getStatut() != Statut.A_COMPLETER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les pièces ne peuvent être modifiées que pour un dossier « À compléter »."
            );
        }
    }

    /**
     * Supprime le nouveau fichier si la transaction SQL est annulée.
     */
    private void supprimerNouveauFichierSiRollback(String nomStockage) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status != STATUS_COMMITTED) {
                            stockage.supprimer(nomStockage);
                        }
                    }
                }
        );
    }

    /**
     * Supprime un ancien fichier uniquement après validation de la transaction.
     */
    private void supprimerApresCommit(String nomStockage) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        stockage.supprimer(nomStockage);
                    }
                }
        );
    }

    /**
     * Nettoie le nom d'origine pour éviter les chemins et caractères indésirables.
     */
    private static String nomAffiche(String nomOriginal) {

        String nom = nomOriginal == null || nomOriginal.isBlank()
                ? "document"
                : nomOriginal;

        nom = nom.replaceAll("[\\\\/\\r\\n\"]", "_").trim();

        return nom.length() > 150
                ? nom.substring(nom.length() - 150)
                : nom;
    }
}
