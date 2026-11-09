package ma.credit360.web.dto;

import java.util.List;

public record PageDto<T>(List<T> contenu, long total, int page, int taille, int nbPages) {
}
