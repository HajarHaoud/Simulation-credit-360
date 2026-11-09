package ma.credit360.service;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import ma.credit360.domain.Dossier;
import ma.credit360.domain.PieceDeposee;
import ma.credit360.domain.PieceRequise;
import ma.credit360.domain.Statut;
import ma.credit360.domain.Utilisateur;
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

    public PieceService(DossierService dossierService, PieceRequiseRepository requises,
                        PieceDeposeeRepository deposees, StockageService stockage, UtilisateurRepository utilisateurs) {
        this.dossierService = dossierService;
        this.requises = requises;
        this.deposees = deposees;
        this.stockage = stockage;
        this.utilisateurs = utilisateurs;
    }

    @Transactional(readOnly = true)
    public ChecklistDto checklist(Utilisateur u, String reference) {
        return construire(dossierService.trouverAccessible(u, reference));
    }

    @Transactional
    public ChecklistDto deposer(Utilisateur u, String reference, Long pieceId, MultipartFile fichier) {
        Dossier d = dossierService.trouverAccessible(u, reference);
        verifierModifiable(d);
        PieceRequise requise = requises.findByIdAndTypeCreditId(pieceId, d.getTypeCredit().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce inconnue pour ce type de crédit."));

        StockageService.Stocke stocke;
        try {
            stocke = stockage.enregistrer(fichier);
        } catch (UncheckedIOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'enregistrement du fichier.");
        }

        PieceDeposee depot = deposees.findByDossierIdAndPieceRequiseId(d.getId(), requise.getId()).orElse(null);
        String ancienFichier = null;
        Utilisateur auteur = utilisateurs.getReferenceById(u.getId());
        String nom = nomAffiche(fichier.getOriginalFilename());
        if (depot == null) {
            depot = new PieceDeposee(d, requise, nom, stocke.typeContenu(), stocke.taille(),
                    stocke.nomStockage(), LocalDateTime.now(), auteur);
        } else {
            ancienFichier = depot.getNomStockage();
            depot.remplacer(nom, stocke.typeContenu(), stocke.taille(), stocke.nomStockage(), LocalDateTime.now(), auteur);
        }
        deposees.save(depot);
        d.setDateMaj(LocalDate.now());
        if (ancienFichier != null) {
            stockage.supprimer(ancienFichier);
        }
        return construire(d);
    }

    @Transactional
    public ChecklistDto supprimer(Utilisateur u, String reference, Long pieceId) {
        Dossier d = dossierService.trouverAccessible(u, reference);
        verifierModifiable(d);
        PieceDeposee depot = deposees.findByDossierIdAndPieceRequiseId(d.getId(), pieceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun fichier déposé pour cette pièce."));
        deposees.delete(depot);
        stockage.supprimer(depot.getNomStockage());
        d.setDateMaj(LocalDate.now());
        return construire(d);
    }

    @Transactional(readOnly = true)
    public Telechargement fichier(Utilisateur u, String reference, Long pieceId) {
        Dossier d = dossierService.trouverAccessible(u, reference);
        PieceDeposee depot = deposees.findByDossierIdAndPieceRequiseId(d.getId(), pieceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun fichier déposé pour cette pièce."));
        var chemin = stockage.chemin(depot.getNomStockage());
        if (!Files.exists(chemin)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable sur le serveur.");
        }
        return new Telechargement(new FileSystemResource(chemin), depot.getNomFichier(), depot.getTypeContenu());
    }

    /** Passe le dossier « À compléter » en « En analyse », à condition que la checklist soit complète. */
    @Transactional
    public DossierDto soumettre(Utilisateur u, String reference) {
        Dossier d = dossierService.trouverAccessible(u, reference);
        if (d.getStatut() != Statut.A_COMPLETER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seuls les dossiers « À compléter » peuvent être soumis à l'analyse.");
        }
        ChecklistDto checklist = construire(d);
        if (!checklist.complete()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pièces manquantes : " + String.join(", ", checklist.manquantes()) + ".");
        }
        d.setStatut(Statut.EN_ANALYSE);
        d.setDateMaj(LocalDate.now());
        return DossierDto.de(d);
    }

    private ChecklistDto construire(Dossier d) {
        List<PieceRequise> liste = requises.findByTypeCreditIdOrderByOrdreAsc(d.getTypeCredit().getId());
        Map<Long, PieceDeposee> parPiece = deposees.findByDossierId(d.getId()).stream()
                .collect(Collectors.toMap(p -> p.getPieceRequise().getId(), Function.identity()));
        List<PieceDto> pieces = liste.stream().map(r -> PieceDto.de(r, parPiece.get(r.getId()))).toList();
        int requisesObligatoires = (int) pieces.stream().filter(PieceDto::obligatoire).count();
        List<String> manquantes = pieces.stream()
                .filter(p -> p.obligatoire() && p.fichier() == null)
                .map(PieceDto::libelle)
                .toList();
        int deposeesObligatoires = requisesObligatoires - manquantes.size();
        return new ChecklistDto(pieces, deposeesObligatoires, requisesObligatoires, manquantes.isEmpty(),
                manquantes, d.getStatut() == Statut.A_COMPLETER);
    }

    private void verifierModifiable(Dossier d) {
        if (d.getStatut() != Statut.A_COMPLETER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les pièces ne peuvent être modifiées que pour un dossier « À compléter ».");
        }
    }

    /** Nettoie le nom d'origine : pas de séparateurs de chemin ni de retours à la ligne, longueur limitée. */
    private static String nomAffiche(String nomOriginal) {
        String nom = nomOriginal == null || nomOriginal.isBlank() ? "document" : nomOriginal;
        nom = nom.replaceAll("[\\\\/\\r\\n\"]", "_").trim();
        return nom.length() > 150 ? nom.substring(nom.length() - 150) : nom;
    }
}
