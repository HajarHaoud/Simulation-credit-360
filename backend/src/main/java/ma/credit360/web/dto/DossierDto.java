package ma.credit360.web.dto;

import java.time.LocalDate;

import ma.credit360.entity.Dossier;
import ma.credit360.entity.Statut;

public record DossierDto(String reference, String client, String clientNom, String clientPrenom, String cin,
                         String telephone, String email, String profession, String adresse, Long revenuMensuel,
                         String typeId, String typeLibelle, long montant, int duree, Statut statut,
                         LocalDate dateMaj) {
    public static DossierDto de(Dossier d) {
        return new DossierDto(d.getReference(), d.getClientPrenom() + " " + d.getClientNom(),
                d.getClientNom(), d.getClientPrenom(), d.getCin(), d.getTelephone(), d.getEmail(),
                d.getProfession(), d.getAdresse(), d.getRevenuMensuel(),
                d.getTypeCredit().getId(), d.getTypeCredit().getLibelle(),
                d.getMontant(), d.getDuree(), d.getStatut(), d.getDateMaj());
    }
}
