package ma.credit360.web.dto;

import java.math.BigDecimal;
import ma.credit360.domain.Famille;
import ma.credit360.domain.TypeCredit;

public record TypeCreditDto(String id, String libelle, Famille famille, BigDecimal taux,
                            long montantMin, long montantMax, int dureeMin, int dureeMax) {
    public static TypeCreditDto de(TypeCredit t) {
        return new TypeCreditDto(t.getId(), t.getLibelle(), t.getFamille(), t.getTaux(),
                t.getMontantMin(), t.getMontantMax(), t.getDureeMin(), t.getDureeMax());
    }
}
