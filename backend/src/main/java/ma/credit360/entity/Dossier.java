package ma.credit360.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dossier")
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference; // CRD00001…

    @Column(name = "client_nom", nullable = false)
    private String clientNom;

    @Column(name = "client_prenom", nullable = false)
    private String clientPrenom;

    @Column(nullable = false)
    private String cin;

    @Column(nullable = false)
    private String telephone;

    private String email;
    private String profession;
    private String adresse;

    @Column(name = "revenu_mensuel")
    private Long revenuMensuel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_credit_id")
    private TypeCredit typeCredit;

    @Column(nullable = false)
    private long montant;

    @Column(nullable = false)
    private int duree; // mois

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.A_COMPLETER;

    @Column(name = "date_maj", nullable = false)
    private LocalDate dateMaj = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_id")
    private Utilisateur creePar;

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }
    public String getClientPrenom() { return clientPrenom; }
    public void setClientPrenom(String clientPrenom) { this.clientPrenom = clientPrenom; }
    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public Long getRevenuMensuel() { return revenuMensuel; }
    public void setRevenuMensuel(Long revenuMensuel) { this.revenuMensuel = revenuMensuel; }
    public TypeCredit getTypeCredit() { return typeCredit; }
    public void setTypeCredit(TypeCredit typeCredit) { this.typeCredit = typeCredit; }
    public long getMontant() { return montant; }
    public void setMontant(long montant) { this.montant = montant; }
    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public LocalDate getDateMaj() { return dateMaj; }
    public void setDateMaj(LocalDate dateMaj) { this.dateMaj = dateMaj; }
    public Utilisateur getCreePar() { return creePar; }
    public void setCreePar(Utilisateur creePar) { this.creePar = creePar; }
}
