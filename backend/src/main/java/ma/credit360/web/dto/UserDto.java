package ma.credit360.web.dto;

import ma.credit360.domain.Role;
import ma.credit360.domain.Utilisateur;

public record UserDto(Long id, String nom, String fonction, String email, Role role) {
    public static UserDto de(Utilisateur u) {
        return new UserDto(u.getId(), u.getNom(), u.getFonction(), u.getEmail(), u.getRole());
    }
}
