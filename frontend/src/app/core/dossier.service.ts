import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Checklist, CreerDossier, Dossier, Page, Stats, TypeCredit, User } from './models';

export interface FiltresDossiers {
  q: string;
  statut: string;
  type: string;
  page: number;
  taille: number;
}

@Injectable({ providedIn: 'root' })
export class DossierService {
  private readonly http = inject(HttpClient);

  types() {
    return this.http.get<TypeCredit[]>('/api/types-credit');
  }

  utilisateurs() {
    return this.http.get<User[]>('/api/utilisateurs');
  }

  lister(f: FiltresDossiers) {
    let params = new HttpParams().set('page', f.page).set('taille', f.taille);
    if (f.q.trim()) params = params.set('q', f.q.trim());
    if (f.statut) params = params.set('statut', f.statut);
    if (f.type) params = params.set('type', f.type);
    return this.http.get<Page<Dossier>>('/api/dossiers', { params });
  }

  stats() {
    return this.http.get<Stats>('/api/dossiers/stats');
  }

  recents() {
    return this.http.get<Dossier[]>('/api/dossiers/recents');
  }

  detail(reference: string) {
    return this.http.get<Dossier>(`/api/dossiers/${reference}`);
  }

  checklist(reference: string) {
    return this.http.get<Checklist>(`/api/dossiers/${reference}/pieces`);
  }

  deposerPiece(reference: string, pieceId: number, fichier: File) {
    const donnees = new FormData();
    donnees.append('fichier', fichier);
    return this.http.post<Checklist>(`/api/dossiers/${reference}/pieces/${pieceId}`, donnees);
  }

  supprimerPiece(reference: string, pieceId: number) {
    return this.http.delete<Checklist>(`/api/dossiers/${reference}/pieces/${pieceId}`);
  }

  fichierPiece(reference: string, pieceId: number) {
    return this.http.get(`/api/dossiers/${reference}/pieces/${pieceId}/fichier`, { responseType: 'blob' });
  }

  soumettre(reference: string) {
    return this.http.post<Dossier>(`/api/dossiers/${reference}/soumettre`, {});
  }

  creer(payload: CreerDossier) {
    return this.http.post<Dossier>('/api/dossiers', payload);
  }
}
