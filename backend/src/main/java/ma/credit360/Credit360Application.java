package ma.credit360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// L'authentification est gérée par notre filtre JWT : on désactive l'utilisateur généré par défaut.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Credit360Application {
    public static void main(String[] args) {
        SpringApplication.run(Credit360Application.class, args);
    }
}
