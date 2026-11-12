package ma.credit360.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.credit360.entity.PieceDeposee;

public interface PieceDeposeeRepository extends JpaRepository<PieceDeposee, Long> {
    List<PieceDeposee> findByDossierId(Long dossierId);

    Optional<PieceDeposee> findByDossierIdAndPieceRequiseId(Long dossierId, Long pieceRequiseId);
}
