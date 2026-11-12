package ma.credit360.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import ma.credit360.entity.Utilisateur;
import ma.credit360.service.PieceService;
import ma.credit360.web.dto.ChecklistDto;
import ma.credit360.web.dto.DossierDto;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dossiers/{reference}")
public class PieceController {

    private final PieceService service;

    public PieceController(PieceService service) {
        this.service = service;
    }

    @GetMapping("/pieces")
    public ChecklistDto checklist(
            @AuthenticationPrincipal Utilisateur u,
            @PathVariable String reference) {

        return service.checklist(u, reference);
    }

    @PostMapping(
            path = "/pieces/{pieceId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ChecklistDto deposer(
            @AuthenticationPrincipal Utilisateur u,
            @PathVariable String reference,
            @PathVariable Long pieceId,
            @RequestParam("fichier") MultipartFile fichier) {

        return service.deposer(u, reference, pieceId, fichier);
    }

    @DeleteMapping("/pieces/{pieceId}")
    public ChecklistDto supprimer(
            @AuthenticationPrincipal Utilisateur u,
            @PathVariable String reference,
            @PathVariable Long pieceId) {

        return service.supprimer(u, reference, pieceId);
    }

    /**
     * Affiche ou télécharge le fichier déposé.
     * Le nom UTF-8 est transmis selon RFC 5987.
     */
    @GetMapping("/pieces/{pieceId}/fichier")
    public ResponseEntity<Resource> fichier(
            @AuthenticationPrincipal Utilisateur u,
            @PathVariable String reference,
            @PathVariable Long pieceId) throws Exception {

        PieceService.Telechargement t =
                service.fichier(u, reference, pieceId);

        Resource ressource = t.ressource();

        String nom = t.nom();

        String nomEncode = URLEncoder
                .encode(nom, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");

        String nomSecours = nom
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("[\\r\\n\"]", "_");

        String disposition = "inline; filename=\""
                + nomSecours
                + "\"; filename*=UTF-8''"
                + nomEncode;

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(t.typeMime());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(ressource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(ressource);
    }

    @PostMapping("/soumettre")
    public DossierDto soumettre(
            @AuthenticationPrincipal Utilisateur u,
            @PathVariable String reference) {

        return service.soumettre(u, reference);
    }
}