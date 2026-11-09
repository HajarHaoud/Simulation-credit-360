package ma.credit360.web;

import java.util.List;
import ma.credit360.repository.TypeCreditRepository;
import ma.credit360.repository.UtilisateurRepository;
import ma.credit360.web.dto.TypeCreditDto;
import ma.credit360.web.dto.UserDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReferentielController {

    private final TypeCreditRepository types;
    private final UtilisateurRepository utilisateurs;

    public ReferentielController(TypeCreditRepository types, UtilisateurRepository utilisateurs) {
        this.types = types;
        this.utilisateurs = utilisateurs;
    }

    @GetMapping("/api/types-credit")
    public List<TypeCreditDto> types() {
        return types.findAllByOrderByFamilleAscLibelleAsc().stream().map(TypeCreditDto::de).toList();
    }

    @GetMapping("/api/utilisateurs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> utilisateurs() {
        return utilisateurs.findAll().stream().map(UserDto::de).toList();
    }
}
