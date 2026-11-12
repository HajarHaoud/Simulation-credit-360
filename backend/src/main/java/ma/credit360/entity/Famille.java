package ma.credit360.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Famille {
    CONSOMMATION("Consommation"), IMMOBILIER("Immobilier");

    private final String libelle;

    Famille(String libelle) {
        this.libelle = libelle;
    }

    @JsonValue
    public String getLibelle() {
        return libelle;
    }
}
