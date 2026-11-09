package ma.credit360.web.dto;

import ma.credit360.domain.PieceDeposee;
import ma.credit360.domain.PieceRequise;

public record PieceDto(Long id, String libelle, boolean obligatoire, FichierDto fichier) {
    public static PieceDto de(PieceRequise r, PieceDeposee depot) {
        FichierDto fichier = depot == null ? null
                : new FichierDto(depot.getNomFichier(), depot.getTaille(), depot.getTypeContenu(), depot.getDateDepot());
        return new PieceDto(r.getId(), r.getLibelle(), r.isObligatoire(), fichier);
    }
}
