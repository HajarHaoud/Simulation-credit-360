package ma.credit360.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ma.credit360.entity.Dossier;
import ma.credit360.entity.Famille;
import ma.credit360.entity.PieceDeposee;
import ma.credit360.entity.PieceRequise;
import ma.credit360.entity.Role;
import ma.credit360.entity.Statut;
import ma.credit360.entity.TypeCredit;
import ma.credit360.entity.Utilisateur;
import ma.credit360.repository.PieceDeposeeRepository;
import ma.credit360.repository.PieceRequiseRepository;
import ma.credit360.repository.TypeCreditRepository;
import ma.credit360.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Initialise types de crédit, pièces requises et, selon la configuration, des données de démonstration. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final TypeCreditRepository types;
    private final PieceRequiseRepository pieces;
    private final PieceDeposeeRepository deposees;
    private final UtilisateurRepository utilisateurs;
    private final DossierService dossiers;
    private final StockageService stockage;
    private final PasswordEncoder encoder;
    private final boolean demo;
    private final String adminEmail;
    private final String adminMotDePasse;

    public DataSeeder(TypeCreditRepository types, PieceRequiseRepository pieces, PieceDeposeeRepository deposees,
                      UtilisateurRepository utilisateurs, DossierService dossiers, StockageService stockage,
                      PasswordEncoder encoder,
                      @Value("${app.seed-demo-data:false}") boolean demo,
                      @Value("${app.admin.initial-email:}") String adminEmail,
                      @Value("${app.admin.initial-password:}") String adminMotDePasse) {
        this.types = types;
        this.pieces = pieces;
        this.deposees = deposees;
        this.utilisateurs = utilisateurs;
        this.dossiers = dossiers;
        this.stockage = stockage;
        this.encoder = encoder;
        this.demo = demo;
        this.adminEmail = adminEmail;
        this.adminMotDePasse = adminMotDePasse;
    }

    @Override
    public void run(String... args) {
        if (types.count() == 0) {
            // Taux et plafonds FICTIFS, à remplacer par les conditions réelles de la banque.
            types.save(new TypeCredit("personnel", "Crédit personnel", Famille.CONSOMMATION, new BigDecimal("7.50"), 10_000, 300_000, 12, 84));
            types.save(new TypeCredit("auto", "Crédit auto", Famille.CONSOMMATION, new BigDecimal("6.90"), 30_000, 500_000, 12, 84));
            types.save(new TypeCredit("equipement", "Crédit équipement", Famille.CONSOMMATION, new BigDecimal("6.50"), 5_000, 150_000, 6, 60));
            types.save(new TypeCredit("immobilier", "Crédit immobilier", Famille.IMMOBILIER, new BigDecimal("4.80"), 200_000, 5_000_000, 60, 300));
        }
        semerPieces();
        if (utilisateurs.count() > 0) {
            return;
        }
        if (demo) {
            semerDemo();
        } else if (!adminEmail.isBlank() && !adminMotDePasse.isBlank()) {
            utilisateurs.save(new Utilisateur("Administrateur", "Administrateur", adminEmail, encoder.encode(adminMotDePasse), Role.ADMIN));
        }
    }

    /** Pièces exigées par type de crédit (liste indicative, à valider avec la banque). */
    private void semerPieces() {
        if (pieces.count() > 0) {
            return;
        }
        for (TypeCredit t : types.findAll()) {
            List<String> obligatoires = new ArrayList<>(List.of(
                    "Copie de la CIN (recto-verso)",
                    "Justificatif de domicile",
                    "Trois derniers bulletins de paie",
                    "Attestation de travail et de salaire",
                    "Relevés bancaires des trois derniers mois"));
            List<String> facultatives = new ArrayList<>();
            switch (t.getId()) {
                case "auto" -> obligatoires.add("Facture pro forma du véhicule");
                case "equipement" -> obligatoires.add("Facture pro forma de l'équipement");
                case "immobilier" -> {
                    obligatoires.add("Promesse de vente ou compromis");
                    obligatoires.add("Titre de propriété du bien");
                    facultatives.add("Dernier avis d'imposition");
                }
                default -> { }
            }
            int ordre = 1;
            for (String libelle : obligatoires) {
                pieces.save(new PieceRequise(t, libelle, true, ordre++));
            }
            for (String libelle : facultatives) {
                pieces.save(new PieceRequise(t, libelle, false, ordre++));
            }
        }
    }

    private void semerDemo() {
        String mdp = encoder.encode("demo1234");
        Utilisateur conseiller = utilisateurs.save(new Utilisateur("Asmaa El Habachi", "Conseillère Commerciale", "conseiller@credit360.ma", mdp, Role.CONSEILLER));
        utilisateurs.save(new Utilisateur("Karim Benali", "Analyste Crédit", "analyste@credit360.ma", mdp, Role.ANALYSTE));
        utilisateurs.save(new Utilisateur("Nadia Lahlou", "Responsable d'agence", "responsable@credit360.ma", mdp, Role.RESPONSABLE));
        utilisateurs.save(new Utilisateur("Youssef Amrani", "Administrateur", "admin@credit360.ma", mdp, Role.ADMIN));

        // prénom, nom, type, montant, durée (mois), statut, jours écoulés depuis la dernière mise à jour
        Object[][] lignes = {
            {"Sara", "Bennani", "immobilier", 520_000L, 204, Statut.VALIDE, 28},
            {"Mehdi", "Alaoui", "auto", 120_000L, 60, Statut.REFUSE, 25},
            {"Imane", "Chraibi", "personnel", 25_000L, 24, Statut.SOLDE, 23},
            {"Omar", "Berrada", "equipement", 30_000L, 24, Statut.SOLDE, 21},
            {"Hassan", "Tazi", "immobilier", 640_000L, 180, Statut.IMPAYE, 18},
            {"Fatima Zahra", "Idrissi", "auto", 150_000L, 60, Statut.IMPAYE, 16},
            {"Rachid", "El Fassi", "personnel", 60_000L, 48, Statut.IMPAYE, 14},
            {"Salma", "Rhazi", "immobilier", 1_200_000L, 276, Statut.A_COMPLETER, 11},
            {"Youssef", "Mannani", "auto", 190_000L, 72, Statut.SIGNE, 10},
            {"Nadia", "Lahbabi", "personnel", 80_000L, 60, Statut.EN_COURS, 8},
            {"Salma", "Amine", "equipement", 45_000L, 36, Statut.VALIDE, 8},
            {"Amine", "Karim", "immobilier", 850_000L, 240, Statut.EN_ANALYSE, 7},
        };
        int i = 0;
        for (Object[] l : lignes) {
            i++;
            Dossier d = new Dossier();
            d.setClientPrenom((String) l[0]);
            d.setClientNom((String) l[1]);
            d.setCin(String.format("AB%06d", 123456 + i * 137));
            d.setTelephone(String.format("06%08d", 12345678 + i * 1111));
            d.setTypeCredit(types.findById((String) l[2]).orElseThrow());
            d.setMontant((Long) l[3]);
            d.setDuree((Integer) l[4]);
            d.setStatut((Statut) l[5]);
            d.setDateMaj(LocalDate.now().minusDays((Integer) l[6]));
            d.setCreePar(conseiller);
            Dossier enregistre = dossiers.enregistrer(d);

            // Un dossier déjà au-delà de « À compléter » a forcément toutes ses pièces obligatoires.
            if (enregistre.getStatut() != Statut.A_COMPLETER) {
                deposerPiecesDemo(enregistre, conseiller);
            }
        }
    }

    private void deposerPiecesDemo(Dossier dossier, Utilisateur auteur) {
        for (PieceRequise piece : pieces.findByTypeCreditIdOrderByOrdreAsc(dossier.getTypeCredit().getId())) {
            if (!piece.isObligatoire()) {
                continue;
            }
            StockageService.Stocke s = stockage.enregistrer(pdfDemo(piece.getLibelle()));
            deposees.save(new PieceDeposee(dossier, piece, "demo-" + piece.getOrdre() + ".pdf", s.typeContenu(),
                    s.taille(), s.nomStockage(), LocalDateTime.now().minusDays(1), auteur));
        }
    }

    /** Petit PDF valide d'une page, uniquement pour la démonstration. */
    private static byte[] pdfDemo(String libelle) {
        String titre = Normalizer.normalize(libelle, Normalizer.Form.NFD).replaceAll("[^\\x20-\\x7E]", "").replaceAll("[()\\\\]", "");
        String flux = "BT /F1 16 Tf 30 110 Td (" + titre + ") Tj ET BT /F1 10 Tf 30 80 Td (Document de demonstration - Credit 360) Tj ET";
        String[] objets = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 420 200] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            "<< /Length " + flux.length() + " >>\nstream\n" + flux + "\nendstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        };
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] positions = new int[objets.length];
        for (int k = 0; k < objets.length; k++) {
            positions[k] = pdf.length();
            pdf.append(k + 1).append(" 0 obj\n").append(objets[k]).append("\nendobj\n");
        }
        int debutXref = pdf.length();
        pdf.append("xref\n0 ").append(objets.length + 1).append("\n0000000000 65535 f \n");
        for (int p : positions) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", p));
        }
        pdf.append("trailer\n<< /Size ").append(objets.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(debutXref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
