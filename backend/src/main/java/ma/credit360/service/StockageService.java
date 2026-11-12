package ma.credit360.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestion du stockage physique des pièces justificatives.
 * Les fichiers sont enregistrés sous un nom UUID généré par le serveur.
 */
@Service
public class StockageService {

    public static final long TAILLE_MAX = 5L * 1024 * 1024;

    public record Stocke(String nomStockage, String typeContenu, long taille) {
    }

    private record Detecte(String typeContenu, String extension) {
    }

    private final Path racine;

    public StockageService(@Value("${app.storage.dir}") String dossier) {
        this.racine = Path.of(dossier).toAbsolutePath().normalize();
    }

    /**
     * Enregistre un fichier reçu depuis l'API.
     */
    public Stocke enregistrer(MultipartFile fichier) {

        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun fichier reçu."
            );
        }

        if (fichier.getSize() > TAILLE_MAX) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Fichier trop volumineux (5 Mo maximum)."
            );
        }

        try (InputStream in = fichier.getInputStream()) {

            byte[] contenu = in.readNBytes((int) TAILLE_MAX + 1);

            if (contenu.length > TAILLE_MAX) {
                throw new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "Fichier trop volumineux (5 Mo maximum)."
                );
            }

            return enregistrer(contenu);

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Impossible de lire le fichier reçu.", e
            );
        }
    }

    /**
     * Enregistre un contenu binaire.
     * Utilisé également par les données de démonstration.
     */
    public Stocke enregistrer(byte[] contenu) {

        if (contenu == null || contenu.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun fichier reçu."
            );
        }

        if (contenu.length > TAILLE_MAX) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Fichier trop volumineux (5 Mo maximum)."
            );
        }

        Detecte type = detecter(contenu);

        if (type == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Format non accepté (PDF, JPG ou PNG uniquement)."
            );
        }

        try {
            Files.createDirectories(racine);

            String nom = UUID.randomUUID() + "." + type.extension();

            Path destination = chemin(nom);

            Files.write(destination, contenu);

            return new Stocke(
                    nom,
                    type.typeContenu(),
                    contenu.length
            );

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Impossible d'enregistrer le fichier.", e
            );
        }
    }

    /**
     * Retourne le chemin physique d'un fichier stocké.
     * Empêche les tentatives de traversée de répertoires.
     */
    public Path chemin(String nomStockage) {

        if (nomStockage == null || nomStockage.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nom de stockage invalide."
            );
        }

        Path chemin = racine.resolve(nomStockage).normalize();

        if (!chemin.startsWith(racine) || chemin.equals(racine)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chemin invalide."
            );
        }

        return chemin;
    }

    /**
     * Supprime physiquement un fichier.
     */
    public void supprimer(String nomStockage) {

        try {
            Files.deleteIfExists(chemin(nomStockage));
        } catch (IOException e) {
            // Une erreur de suppression ne doit pas annuler une opération métier.
            // Le nettoyage des fichiers orphelins sera traité séparément.
        }
    }

    /**
     * Vérifie la signature réelle du fichier.
     * L'extension et le Content-Type déclarés par le navigateur ne sont pas fiables.
     */
    private static Detecte detecter(byte[] b) {

        // PDF : %PDF
        if (b.length >= 4
                && b[0] == '%'
                && b[1] == 'P'
                && b[2] == 'D'
                && b[3] == 'F') {

            return new Detecte("application/pdf", "pdf");
        }

        // JPEG
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF) {

            return new Detecte("image/jpeg", "jpg");
        }

        // PNG : signature complète sur 8 octets
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89
                && b[1] == 'P'
                && b[2] == 'N'
                && b[3] == 'G'
                && b[4] == 0x0D
                && b[5] == 0x0A
                && b[6] == 0x1A
                && b[7] == 0x0A) {

            return new Detecte("image/png", "png");
        }

        return null;
    }
}
