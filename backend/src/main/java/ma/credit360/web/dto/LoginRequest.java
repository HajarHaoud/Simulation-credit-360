package ma.credit360.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'adresse e-mail est obligatoire.") String email,
        @NotBlank(message = "Le mot de passe est obligatoire.") String motDePasse) {
}
