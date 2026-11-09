export type Role = 'conseiller' | 'analyste' | 'responsable' | 'admin';

export interface User {
  id: number;
  nom: string;
  fonction: string;
  email: string;
  role: Role;
}

export const STATUTS = [
  'À compléter', 'En analyse', 'En cours', 'Validé', 'Refusé', 'Signé', 'Impayé', 'Soldé',
] as const;
export type Statut = (typeof STATUTS)[number];

export type Famille = 'Consommation' | 'Immobilier';

export interface TypeCredit {
  id: string;
  libelle: string;
  famille: Famille;
  taux: number;        // taux annuel en %
  montantMin: number;
  montantMax: number;
  dureeMin: number;    // en mois
  dureeMax: number;
}

export interface Dossier {
  reference: string;
  client: string;
  clientNom: string;
  clientPrenom: string;
  cin: string;
  telephone: string;
  email: string | null;
  profession: string | null;
  adresse: string | null;
  revenuMensuel: number | null;
  typeId: string;
  typeLibelle: string;
  montant: number;
  duree: number;
  statut: Statut;
  dateMaj: string;     // ISO yyyy-MM-dd
}

export interface Page<T> {
  contenu: T[];
  total: number;
  page: number;
  taille: number;
  nbPages: number;
}

export interface Stats {
  enCours: number;
  valides: number;
  impayes: number;
  soldes: number;
}

export interface CreerDossier {
  nom: string;
  prenom: string;
  cin: string;
  telephone: string;
  email: string | null;
  profession: string | null;
  adresse: string | null;
  revenuMensuel: number | null;
  typeId: string;
  montant: number;
  duree: number;
}

export interface Fichier {
  nom: string;
  taille: number;
  typeContenu: string;
  dateDepot: string;   // ISO date-heure
}

export interface Piece {
  id: number;
  libelle: string;
  obligatoire: boolean;
  fichier: Fichier | null;
}

export interface Checklist {
  pieces: Piece[];
  deposees: number;     // pièces obligatoires déposées
  requises: number;     // pièces obligatoires
  complete: boolean;
  manquantes: string[];
  modifiable: boolean;
}
