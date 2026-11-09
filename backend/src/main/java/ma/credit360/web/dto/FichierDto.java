package ma.credit360.web.dto;

import java.time.LocalDateTime;

public record FichierDto(String nom, long taille, String typeContenu, LocalDateTime dateDepot) {
}
