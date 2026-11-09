package ma.credit360.web;

import jakarta.validation.Valid;
import ma.credit360.domain.Utilisateur;
import ma.credit360.repository.UtilisateurRepository;
import ma.credit360.security.JwtService;
import ma.credit360.web.dto.LoginRequest;
import ma.credit360.web.dto.LoginResponse;
import ma.credit360.web.dto.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UtilisateurRepository utilisateurs, PasswordEncoder encoder, JwtService jwt) {
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Utilisateur u = utilisateurs.findByEmailIgnoreCase(req.email().trim())
                .filter(Utilisateur::isActif)
                .filter(x -> encoder.matches(req.motDePasse(), x.getMotDePasse()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects."));
        return new LoginResponse(jwt.generer(u), UserDto.de(u));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Utilisateur u) {
        return UserDto.de(u);
    }
}
