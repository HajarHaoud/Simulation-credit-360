package ma.credit360.web.dto;

import java.util.List;

/**
 * @param deposees   nombre de pièces obligatoires déjà déposées
 * @param requises   nombre de pièces obligatoires
 * @param complete   toutes les pièces obligatoires sont présentes
 * @param manquantes libellés des pièces obligatoires encore manquantes
 * @param modifiable les dépôts et suppressions sont autorisés (dossier « À compléter »)
 */
public record ChecklistDto(List<PieceDto> pieces, int deposees, int requises, boolean complete,
                           List<String> manquantes, boolean modifiable) {
}
