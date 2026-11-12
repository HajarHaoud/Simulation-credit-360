package ma.credit360.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.credit360.entity.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
}
