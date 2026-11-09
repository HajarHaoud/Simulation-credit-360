package ma.credit360.repository;

import java.util.Optional;
import ma.credit360.domain.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DossierRepository extends JpaRepository<Dossier, Long>, JpaSpecificationExecutor<Dossier> {
    Optional<Dossier> findByReference(String reference);
}
