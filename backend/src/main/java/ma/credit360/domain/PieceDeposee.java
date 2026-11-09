package ma.credit360.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Fichier déposé pour une pièce requise d'un dossier (un seul fichier par pièce : un nouveau dépôt remplace l'ancien). */
@Entity
@Table(name = "piece_deposee",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dossier_id", "piece_requise_id"}))
public class PieceDeposee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "piece_requise_id")
    private PieceRequise pieceRequise;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;     // nom d'origine, affiché à l'utilisateur

    @Column(name = "type_contenu", nullable = false)
    private String typeContenu;

    @Column(nullable = false)
    private long taille;

    @Column(name = "nom_stockage", nullable = false)
    private String nomStockage;    // nom généré (UUID) sur le disque

    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depose_par_id")
    private Utilisateur deposePar;

    protected PieceDeposee() {
    }

    public PieceDeposee(Dossier dossier, PieceRequise pieceRequise, String nomFichier, String typeContenu,
                        long taille, String nomStockage, LocalDateTime dateDepot, Utilisateur deposePar) {
        this.dossier = dossier;
        this.pieceRequise = pieceRequise;
        remplacer(nomFichier, typeContenu, taille, nomStockage, dateDepot, deposePar);
    }

    public void remplacer(String nomFichier, String typeContenu, long taille, String nomStockage,
                          LocalDateTime dateDepot, Utilisateur deposePar) {
        this.nomFichier = nomFichier;
        this.typeContenu = typeContenu;
        this.taille = taille;
        this.nomStockage = nomStockage;
        this.dateDepot = dateDepot;
        this.deposePar = deposePar;
    }

    public Long getId() { return id; }
    public Dossier getDossier() { return dossier; }
    public PieceRequise getPieceRequise() { return pieceRequise; }
    public String getNomFichier() { return nomFichier; }
    public String getTypeContenu() { return typeContenu; }
    public long getTaille() { return taille; }
    public String getNomStockage() { return nomStockage; }
    public LocalDateTime getDateDepot() { return dateDepot; }
}
