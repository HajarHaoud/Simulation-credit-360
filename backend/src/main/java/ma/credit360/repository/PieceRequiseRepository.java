package ma.credit360.repository;

import java.util.List;
import java.util.Optional;
import ma.credit360.domain.PieceRequise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PieceRequiseRepository extends JpaRepository<PieceRequise, Long> {
    List<PieceRequise> findByTypeCreditIdOrderByOrdreAsc(String typeCreditId);

    Optional<PieceRequise> findByIdAndTypeCreditId(Long id, String typeCreditId);
}
