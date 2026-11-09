import { TypeCredit } from './models';

/** Valeurs d'attente affichées le temps que l'API renvoie les vrais types de crédit. */
export const TYPE_ATTENTE: TypeCredit = {
  id: 'personnel', libelle: 'Crédit personnel', famille: 'Consommation', taux: 7.5,
  montantMin: 10000, montantMax: 300000, dureeMin: 12, dureeMax: 84,
};
