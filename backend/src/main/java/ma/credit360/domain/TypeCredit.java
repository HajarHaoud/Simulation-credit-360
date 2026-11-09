package ma.credit360.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "type_credit")
public class TypeCredit {

    @Id
    private String id; // ex. « personnel », « immobilier »

    @Column(nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Famille famille;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taux; // taux annuel en %

    @Column(name = "montant_min", nullable = false)
    private long montantMin;

    @Column(name = "montant_max", nullable = false)
    private long montantMax;

    @Column(name = "duree_min", nullable = false)
    private int dureeMin; // mois

    @Column(name = "duree_max", nullable = false)
    private int dureeMax;

    protected TypeCredit() {
    }

    public TypeCredit(String id, String libelle, Famille famille, BigDecimal taux,
                      long montantMin, long montantMax, int dureeMin, int dureeMax) {
        this.id = id;
        this.libelle = libelle;
        this.famille = famille;
        this.taux = taux;
        this.montantMin = montantMin;
        this.montantMax = montantMax;
        this.dureeMin = dureeMin;
        this.dureeMax = dureeMax;
    }

    public String getId() { return id; }
    public String getLibelle() { return libelle; }
    public Famille getFamille() { return famille; }
    public BigDecimal getTaux() { return taux; }
    public long getMontantMin() { return montantMin; }
    public long getMontantMax() { return montantMax; }
    public int getDureeMin() { return dureeMin; }
    public int getDureeMax() { return dureeMax; }
}
