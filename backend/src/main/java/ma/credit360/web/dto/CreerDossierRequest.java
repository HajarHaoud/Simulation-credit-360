package ma.credit360.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreerDossierRequest(
        @NotBlank(message = "Le nom est obligatoire.") String nom,
        @NotBlank(message = "Le prénom est obligatoire.") String prenom,
        @NotBlank(message = "Le CIN est obligatoire.") String cin,
        @NotBlank(message = "Le téléphone est obligatoire.") String telephone,
        @Email(message = "Adresse e-mail invalide.") String email,
        String profession,
        String adresse,
        @PositiveOrZero(message = "Le revenu ne peut pas être négatif.") Long revenuMensuel,
        @NotBlank(message = "Le type de crédit est obligatoire.") String typeId,
        @Positive(message = "Le montant doit être positif.") long montant,
        @Positive(message = "La durée doit être positive.") int duree) {
}
