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

/** Stockage des fichiers sur disque. Le nom d'origine n'est jamais utilisé comme chemin : on génère un nom UUID. */
@Service
public class StockageService {

    public static final long TAILLE_MAX = 5L * 1024 * 1024; // 5 Mo

    public record Stocke(String nomStockage, String typeContenu, long taille) {
    }

    private record Detecte(String typeContenu, String extension) {
    }

    private final Path racine;

    public StockageService(@Value("${app.storage.dir}") String dossier) {
        this.racine = Path.of(dossier).toAbsolutePath().normalize();
    }

    /** Enregistre un fichier reçu par l'API après contrôle de sa taille et de son format réel. */
    public Stocke enregistrer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier reçu.");
        }
        try (InputStream in = fichier.getInputStream()) {
            return enregistrer(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Enregistre un contenu en mémoire (utilisé aussi par les données de démonstration). */
    public Stocke enregistrer(byte[] contenu) {
        if (contenu.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier reçu.");
        }
        if (contenu.length > TAILLE_MAX) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux (5 Mo maximum).");
        }
        // Le format est déterminé par le contenu réel, pas par l'extension ni par le type annoncé par le navigateur.
        Detecte type = detecter(contenu);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format non accepté (PDF, JPG ou PNG uniquement).");
        }
        try {
            Files.createDirectories(racine);
            String nom = UUID.randomUUID() + "." + type.extension();
            Files.write(racine.resolve(nom), contenu);
            return new Stocke(nom, type.typeContenu(), contenu.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path chemin(String nomStockage) {
        Path chemin = racine.resolve(nomStockage).normalize();
        if (!chemin.startsWith(racine)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chemin invalide.");
        }
        return chemin;
    }

    public void supprimer(String nomStockage) {
        try {
            Files.deleteIfExists(chemin(nomStockage));
        } catch (IOException e) {
            // Suppression « au mieux » : un fichier orphelin n'empêche pas l'opération métier.
        }
    }

    private static Detecte detecter(byte[] b) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return new Detecte("application/pdf", "pdf");
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return new Detecte("image/jpeg", "jpg");
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return new Detecte("image/png", "png");
        }
        return null;
    }
}
