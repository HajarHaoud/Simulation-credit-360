package ma.credit360.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String fonction;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse; // hash BCrypt

    @Enumerated(EnumType.STRING)
    @Column(name = "role_utilisateur", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean actif = true;

    protected Utilisateur() {
    }

    public Utilisateur(String nom, String fonction, String email, String motDePasseHash, Role role) {
        this.nom = nom;
        this.fonction = fonction;
        this.email = email.trim().toLowerCase();
        this.motDePasse = motDePasseHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getFonction() { return fonction; }
    public String getEmail() { return email; }
    public String getMotDePasse() { return motDePasse; }
    public Role getRole() { return role; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
