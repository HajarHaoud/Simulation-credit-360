package ma.credit360.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> champs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> champs.putIfAbsent(e.getField(), e.getDefaultMessage()));
        String premier = champs.values().stream().findFirst().orElse("Données invalides.");
        return ResponseEntity.badRequest().body(Map.of("message", premier, "champs", champs));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> statut(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Erreur.";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> tropVolumineux(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413).body(Map.of("message", "Fichier trop volumineux (5 Mo maximum)."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> refuse(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("message", "Accès refusé."));
    }
}
