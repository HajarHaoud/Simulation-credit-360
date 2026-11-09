package ma.credit360.domain;

import jakarta.persistence.*;

/** Pièce à fournir pour un type de crédit donné (configuration). */
@Entity
@Table(name = "piece_requise")
public class PieceRequise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_credit_id")
    private TypeCredit typeCredit;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private boolean obligatoire = true;

    @Column(nullable = false)
    private int ordre;

    protected PieceRequise() {
    }

    public PieceRequise(TypeCredit typeCredit, String libelle, boolean obligatoire, int ordre) {
        this.typeCredit = typeCredit;
        this.libelle = libelle;
        this.obligatoire = obligatoire;
        this.ordre = ordre;
    }

    public Long getId() { return id; }
    public TypeCredit getTypeCredit() { return typeCredit; }
    public String getLibelle() { return libelle; }
    public boolean isObligatoire() { return obligatoire; }
    public int getOrdre() { return ordre; }
}
