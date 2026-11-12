package ma.credit360.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum Role {
    CONSEILLER, ANALYSTE, RESPONSABLE, ADMIN;

    /** Sérialisé en minuscules pour le front (« conseiller », « admin »…). */
    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }
}
