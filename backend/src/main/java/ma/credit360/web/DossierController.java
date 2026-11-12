package ma.credit360.web;

import jakarta.validation.Valid;
import java.util.List;

import ma.credit360.entity.Statut;
import ma.credit360.entity.Utilisateur;
import ma.credit360.service.DossierService;
import ma.credit360.web.dto.CreerDossierRequest;
import ma.credit360.web.dto.DossierDto;
import ma.credit360.web.dto.PageDto;
import ma.credit360.web.dto.StatsDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    private final DossierService service;

    public DossierController(DossierService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<DossierDto> lister(@AuthenticationPrincipal Utilisateur u,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) String statut,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "8") int taille) {
        Statut s = null;
        if (statut != null && !statut.isBlank()) {
            s = Statut.depuisLibelle(statut)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu."));
        }
        int tailleBornee = Math.min(Math.max(taille, 1), 50);
        return service.lister(u, q, s, type, page, tailleBornee);
    }

    @GetMapping("/stats")
    public StatsDto stats(@AuthenticationPrincipal Utilisateur u) {
        return service.stats(u);
    }

    @GetMapping("/recents")
    public List<DossierDto> recents(@AuthenticationPrincipal Utilisateur u) {
        return service.recents(u);
    }

    @GetMapping("/{reference}")
    public DossierDto detail(@AuthenticationPrincipal Utilisateur u, @PathVariable String reference) {
        return service.detail(u, reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DossierDto creer(@AuthenticationPrincipal Utilisateur u, @Valid @RequestBody CreerDossierRequest req) {
        return service.creer(u, req);
    }
}
