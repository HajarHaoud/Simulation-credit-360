package ma.credit360.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import ma.credit360.entity.Utilisateur;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey cle;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secretBase64,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.cle = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expirationMinutes = expirationMinutes;
    }

    public String generer(Utilisateur utilisateur) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(utilisateur.getEmail())
                .claim("role", utilisateur.getRole().name())
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(cle)
                .compact();
    }

    /** Retourne l'e-mail contenu dans le jeton, ou vide si le jeton est invalide ou expiré. */
    public Optional<String> emailDepuis(String jeton) {
        try {
            Claims claims = Jwts.parser().verifyWith(cle).build().parseSignedClaims(jeton).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
