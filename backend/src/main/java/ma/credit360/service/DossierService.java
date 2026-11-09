package ma.credit360.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import ma.credit360.domain.Dossier;
import ma.credit360.domain.Role;
import ma.credit360.domain.Statut;
import ma.credit360.domain.TypeCredit;
import ma.credit360.domain.Utilisateur;
import ma.credit360.repository.DossierRepository;
import ma.credit360.repository.TypeCreditRepository;
import ma.credit360.repository.UtilisateurRepository;
import ma.credit360.web.dto.CreerDossierRequest;
import ma.credit360.web.dto.DossierDto;
import ma.credit360.web.dto.PageDto;
import ma.credit360.web.dto.StatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DossierService {

    private static final Sort TRI = Sort.by(Sort.Direction.DESC, "dateMaj").and(Sort.by(Sort.Direction.DESC, "id"));

    private final DossierRepository dossiers;
    private final TypeCreditRepository types;
    private final UtilisateurRepository utilisateurs;

    public DossierService(DossierRepository dossiers, TypeCreditRepository types, UtilisateurRepository utilisateurs) {
        this.dossiers = dossiers;
        this.types = types;
        this.utilisateurs = utilisateurs;
    }

    /** Un conseiller ne voit que ses dossiers ; les autres rôles voient tout. */
    private Specification<Dossier> portee(Utilisateur u) {
        if (u.getRole() == Role.CONSEILLER) {
            return (root, query, cb) -> cb.equal(root.get("creePar").get("id"), u.getId());
        }
        return (root, query, cb) -> cb.conjunction();
    }

    @Transactional(readOnly = true)
    public PageDto<DossierDto> lister(Utilisateur u, String q, Statut statut, String typeId, int page, int taille) {
        Specification<Dossier> spec = portee(u);
        if (statut != null) {
            spec = spec.and((r, qr, cb) -> cb.equal(r.get("statut"), statut));
        }
        if (typeId != null && !typeId.isBlank()) {
            spec = spec.and((r, qr, cb) -> cb.equal(r.get("typeCredit").get("id"), typeId));
        }
        if (q != null && !q.isBlank()) {
            String motif = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((r, qr, cb) -> cb.or(
                    cb.like(cb.lower(r.<String>get("reference")), motif),
                    cb.like(cb.lower(r.<String>get("clientNom")), motif),
                    cb.like(cb.lower(r.<String>get("clientPrenom")), motif),
                    cb.like(cb.lower(cb.concat(cb.concat(r.<String>get("clientPrenom"), " "), r.<String>get("clientNom"))), motif)));
        }
        int pageIndex = Math.max(page, 1) - 1;
        Page<Dossier> resultat = dossiers.findAll(spec, PageRequest.of(pageIndex, taille, TRI));
        List<DossierDto> contenu = resultat.getContent().stream().map(DossierDto::de).toList();
        return new PageDto<>(contenu, resultat.getTotalElements(), pageIndex + 1, taille, resultat.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<DossierDto> recents(Utilisateur u) {
        return dossiers.findAll(portee(u), PageRequest.of(0, 5, TRI)).getContent().stream().map(DossierDto::de).toList();
    }

    @Transactional(readOnly = true)
    public StatsDto stats(Utilisateur u) {
        return new StatsDto(
                compter(u, Statut.OUVERTS),
                compter(u, Statut.VALIDES),
                compter(u, Set.of(Statut.IMPAYE)),
                compter(u, Set.of(Statut.SOLDE)));
    }

    private long compter(Utilisateur u, Set<Statut> statuts) {
        return dossiers.count(portee(u).and((r, q, cb) -> r.get("statut").in(statuts)));
    }

    @Transactional(readOnly = true)
    public DossierDto detail(Utilisateur u, String reference) {
        return DossierDto.de(trouverAccessible(u, reference));
    }

    /** Dossier visible par l'utilisateur, sinon 404 (on ne révèle pas l'existence d'un dossier d'un autre conseiller). À appeler dans une transaction. */
    public Dossier trouverAccessible(Utilisateur u, String reference) {
        return dossiers.findByReference(reference)
                .filter(x -> u.getRole() != Role.CONSEILLER || x.getCreePar().getId().equals(u.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable."));
    }

    @Transactional
    public DossierDto creer(Utilisateur u, CreerDossierRequest req) {
        TypeCredit type = types.findById(req.typeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de crédit inconnu."));
        if (req.montant() < type.getMontantMin() || req.montant() > type.getMontantMax()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Locale.FRANCE,
                    "Le montant doit être compris entre %,d et %,d DH.", type.getMontantMin(), type.getMontantMax()));
        }
        if (req.duree() < type.getDureeMin() || req.duree() > type.getDureeMax()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                    "La durée doit être comprise entre %d et %d mois.", type.getDureeMin(), type.getDureeMax()));
        }

        Dossier d = new Dossier();
        d.setClientNom(req.nom().trim());
        d.setClientPrenom(req.prenom().trim());
        d.setCin(req.cin().trim());
        d.setTelephone(req.telephone().trim());
        d.setEmail(vide(req.email()));
        d.setProfession(vide(req.profession()));
        d.setAdresse(vide(req.adresse()));
        d.setRevenuMensuel(req.revenuMensuel());
        d.setTypeCredit(type);
        d.setMontant(req.montant());
        d.setDuree(req.duree());
        d.setStatut(Statut.A_COMPLETER);
        d.setDateMaj(LocalDate.now());
        d.setCreePar(utilisateurs.getReferenceById(u.getId()));
        return DossierDto.de(enregistrer(d));
    }

    /**
     * Enregistre le dossier et lui attribue sa référence (CRD + identifiant sur 5 chiffres).
     * Une référence provisoire est posée pour respecter la contrainte d'unicité, puis remplacée dans la même transaction.
     */
    @Transactional
    public Dossier enregistrer(Dossier d) {
        d.setReference("TMP-" + UUID.randomUUID());
        Dossier enregistre = dossiers.saveAndFlush(d);
        enregistre.setReference(String.format("CRD%05d", enregistre.getId()));
        return enregistre;
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
