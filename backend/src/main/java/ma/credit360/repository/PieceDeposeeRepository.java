package ma.credit360.repository;

import java.util.List;
import java.util.Optional;
import ma.credit360.domain.PieceDeposee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PieceDeposeeRepository extends JpaRepository<PieceDeposee, Long> {
    List<PieceDeposee> findByDossierId(Long dossierId);

    Optional<PieceDeposee> findByDossierIdAndPieceRequiseId(Long dossierId, Long pieceRequiseId);
}
