package ma.credit360.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import ma.credit360.entity.Utilisateur;
import ma.credit360.repository.UtilisateurRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lit l'en-tête « Authorization: Bearer … » et authentifie la requête. */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurs;

    public JwtAuthFilter(JwtService jwtService, UtilisateurRepository utilisateurs) {
        this.jwtService = jwtService;
        this.utilisateurs = utilisateurs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String entete = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete != null && entete.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.emailDepuis(entete.substring(7))
                    .flatMap(utilisateurs::findByEmailIgnoreCase)
                    .filter(Utilisateur::isActif)
                    .ifPresent(u -> {
                        var authentification = new UsernamePasswordAuthenticationToken(
                                u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentification);
                    });
        }
        chain.doFilter(request, response);
    }
}
