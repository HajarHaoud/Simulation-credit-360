package ma.credit360.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.credit360.entity.TypeCredit;

public interface TypeCreditRepository extends JpaRepository<TypeCredit, String> {
    List<TypeCredit> findAllByOrderByFamilleAscLibelleAsc();
}
