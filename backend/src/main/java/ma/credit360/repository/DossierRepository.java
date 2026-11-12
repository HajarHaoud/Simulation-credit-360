package ma.credit360.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ma.credit360.entity.Dossier;

public interface DossierRepository extends JpaRepository<Dossier, Long>, JpaSpecificationExecutor<Dossier> {
    Optional<Dossier> findByReference(String reference);
}
