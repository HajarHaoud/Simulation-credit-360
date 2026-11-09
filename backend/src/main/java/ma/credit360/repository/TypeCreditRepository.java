package ma.credit360.repository;

import java.util.List;
import ma.credit360.domain.TypeCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TypeCreditRepository extends JpaRepository<TypeCredit, String> {
    List<TypeCredit> findAllByOrderByFamilleAscLibelleAsc();
}
