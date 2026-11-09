package ma.credit360.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum Statut {
    A_COMPLETER("À compléter"), EN_ANALYSE("En analyse"), EN_COURS("En cours"), VALIDE("Validé"),
    REFUSE("Refusé"), SIGNE("Signé"), IMPAYE("Impayé"), SOLDE("Soldé");

    /** Dossiers considérés « en cours » dans le tableau de bord. */
    public static final Set<Statut> OUVERTS = EnumSet.of(A_COMPLETER, EN_ANALYSE, EN_COURS);
    public static final Set<Statut> VALIDES = EnumSet.of(VALIDE, SIGNE);

    private final String libelle;

    Statut(String libelle) {
        this.libelle = libelle;
    }

    @JsonValue
    public String getLibelle() {
        return libelle;
    }

    public static Optional<Statut> depuisLibelle(String libelle) {
        return Arrays.stream(values()).filter(s -> s.libelle.equalsIgnoreCase(libelle)).findFirst();
    }
}
