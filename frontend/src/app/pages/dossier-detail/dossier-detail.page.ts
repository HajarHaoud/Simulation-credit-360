import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DossierService } from '../../core/dossier.service';
import { Checklist, Dossier, Piece } from '../../core/models';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { IconComponent } from '../../shared/icon.component';

const EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png'];
const TAILLE_MAX = 5 * 1024 * 1024;

@Component({
  selector: 'app-dossier-detail',
  imports: [RouterLink, DatePipe, DecimalPipe, StatusBadgeComponent, IconComponent],
  template: `
    <a class="retour" routerLink="/dossiers">← Mes dossiers</a>

    @if (erreur()) { <div class="alert warn">{{ erreur() }}</div> }
    @if (message()) { <div class="alert ok">{{ message() }}</div> }

    @if (dossier(); as d) {
      <h1 class="page-title">{{ d.reference }} <app-status-badge [statut]="d.statut" /></h1>
      <p class="page-sub">{{ d.client }} · {{ d.typeLibelle }} · {{ d.montant | number: '1.0-0' }} DH sur {{ d.duree }} mois</p>

      @if (cree && d.statut === 'À compléter' && !message()) {
        <div class="alert ok">Dossier {{ cree }} créé. Déposez maintenant les pièces justificatives.</div>
      }

      <div class="onglets">
        <button type="button" [class.actif]="onglet() === 'infos'" (click)="onglet.set('infos')">Informations</button>
        <button type="button" [class.actif]="onglet() === 'pieces'" (click)="onglet.set('pieces')">
          Pièces @if (checklist(); as c) { <span class="pastille" [class.ok]="c.complete">{{ c.deposees }}/{{ c.requises }}</span> }
        </button>
      </div>

      @if (onglet() === 'infos') {
        <div class="grid duo">
          <section class="card">
            <h2>Client</h2>
            <dl>
              <dt>Nom complet</dt><dd>{{ d.clientPrenom }} {{ d.clientNom }}</dd>
              <dt>CIN</dt><dd>{{ d.cin }}</dd>
              <dt>Téléphone</dt><dd>{{ d.telephone }}</dd>
              <dt>E-mail</dt><dd>{{ d.email || '—' }}</dd>
              <dt>Profession</dt><dd>{{ d.profession || '—' }}</dd>
              <dt>Adresse</dt><dd>{{ d.adresse || '—' }}</dd>
              <dt>Revenu mensuel net</dt><dd>@if (d.revenuMensuel !== null) { {{ d.revenuMensuel | number: '1.0-0' }} DH } @else { — }</dd>
            </dl>
          </section>
          <section class="card">
            <h2>Crédit</h2>
            <dl>
              <dt>Type</dt><dd>{{ d.typeLibelle }}</dd>
              <dt>Montant</dt><dd>{{ d.montant | number: '1.0-0' }} DH</dd>
              <dt>Durée</dt><dd>{{ d.duree }} mois</dd>
              <dt>Statut</dt><dd><app-status-badge [statut]="d.statut" /></dd>
              <dt>Dernière mise à jour</dt><dd>{{ d.dateMaj | date: 'dd/MM/yyyy' }}</dd>
            </dl>
          </section>
        </div>
      } @else {
        @if (checklist(); as c) {
          <section class="card">
            <div class="prog-tete">
              <h2>Pièces justificatives</h2>
              <strong>{{ c.deposees }}/{{ c.requises }} pièces obligatoires</strong>
            </div>
            <div class="barre"><span [style.width.%]="progression()"></span></div>

            @if (c.complete) {
              <div class="alert ok">La checklist est complète.</div>
            } @else {
              <div class="alert warn">Pièces manquantes : {{ c.manquantes.join(', ') }}.</div>
            }
            @if (!c.modifiable) {
              <p class="muted">Le dossier n'est plus « À compléter » : les pièces ne peuvent plus être modifiées.</p>
            }

            <ul class="pieces">
              @for (p of c.pieces; track p.id) {
                <li [class.depose]="p.fichier">
                  <span class="etat" [class.ok]="p.fichier">
                    @if (p.fichier) { <app-icon name="check" /> } @else { <app-icon name="x" /> }
                  </span>
                  <div class="info">
                    <strong>{{ p.libelle }}</strong>
                    @if (!p.obligatoire) { <span class="fac">Facultatif</span> }
                    @if (p.fichier; as fic) {
                      <small class="muted">{{ fic.nom }} · {{ taille(fic.taille) }} · déposé le {{ fic.dateDepot | date: 'dd/MM/yyyy HH:mm' }}</small>
                    }
                  </div>
                  <div class="actions-piece">
                    @if (p.fichier) {
                      <button type="button" class="btn" (click)="voir(p)"><app-icon name="eye" /> Voir</button>
                    }
                    @if (c.modifiable) {
                      <label class="btn" [class.inactif]="occupe() !== null">
                        <app-icon name="upload" /> {{ occupe() === p.id ? 'Envoi…' : (p.fichier ? 'Remplacer' : 'Déposer') }}
                        <input type="file" hidden accept=".pdf,.jpg,.jpeg,.png" #champ
                               [disabled]="occupe() !== null" (change)="choisir(p, champ)" />
                      </label>
                      @if (p.fichier) {
                        <button type="button" class="btn" title="Supprimer" [disabled]="occupe() !== null" (click)="supprimer(p)">
                          <app-icon name="trash" />
                        </button>
                      }
                    }
                  </div>
                </li>
              }
            </ul>
            <p class="muted aide">Formats acceptés : PDF, JPG, PNG — 5 Mo maximum par fichier.</p>

            @if (c.modifiable) {
              <div class="soumission">
                <button type="button" class="btn btn-primary" [disabled]="!c.complete || occupe() !== null" (click)="soumettre()">
                  {{ occupe() === 'soumission' ? 'Envoi…' : "Soumettre à l'analyse" }}
                </button>
                @if (!c.complete) { <span class="muted">Disponible quand toutes les pièces obligatoires sont déposées.</span> }
              </div>
            }
          </section>
        }
      }
    }
  `,
  styles: [`
    .retour { color: var(--muted); text-decoration: none; display: inline-block; margin-bottom: 10px; }
    .retour:hover { color: var(--orange-dark); }
    .page-title { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .onglets { display: flex; gap: 4px; border-bottom: 1px solid var(--bord); margin-bottom: 16px; }
    .onglets button { background: none; border: 0; border-bottom: 3px solid transparent; padding: 10px 16px; font: inherit; font-weight: 600; color: var(--muted); cursor: pointer; }
    .onglets button.actif { color: var(--texte); border-bottom-color: var(--orange); }
    .pastille { margin-left: 6px; background: var(--warn-bg); color: var(--warn); border-radius: 999px; padding: 1px 8px; font-size: 12px; }
    .pastille.ok { background: var(--ok-bg); color: var(--ok); }
    .duo { grid-template-columns: 1fr 1fr; }
    dl { display: grid; grid-template-columns: 170px 1fr; gap: 8px 16px; margin: 0; }
    dt { color: var(--muted); } dd { margin: 0; font-weight: 600; }
    .prog-tete { display: flex; justify-content: space-between; align-items: baseline; }
    .barre { height: 8px; background: var(--bord); border-radius: 999px; overflow: hidden; margin: 6px 0 14px; }
    .barre span { display: block; height: 100%; background: var(--orange); transition: width .3s; }
    .pieces { list-style: none; margin: 0; padding: 0; }
    .pieces li { display: flex; align-items: center; gap: 14px; padding: 12px 4px; border-bottom: 1px solid var(--bord); }
    .pieces li:last-child { border-bottom: 0; }
    .etat { width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center; background: var(--ko-bg); color: var(--ko); flex-shrink: 0; }
    .etat.ok { background: var(--ok-bg); color: var(--ok); }
    .info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .info small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .fac { font-size: 11px; color: var(--muted); border: 1px solid var(--bord); border-radius: 999px; padding: 0 8px; align-self: flex-start; }
    .actions-piece { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
    label.btn.inactif { opacity: .5; pointer-events: none; }
    .aide { margin: 12px 0 0; font-size: 12.5px; }
    .soumission { display: flex; align-items: center; gap: 14px; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--bord); }
    @media (max-width: 900px) { .duo { grid-template-columns: 1fr; } .pieces li { flex-wrap: wrap; } }
  `],
})
export class DossierDetailPage {
  private readonly service = inject(DossierService);
  private readonly route = inject(ActivatedRoute);

  protected readonly reference = this.route.snapshot.paramMap.get('reference') ?? '';
  protected readonly cree = this.route.snapshot.queryParamMap.get('cree');
  protected readonly onglet = signal<'infos' | 'pieces'>(
    this.route.snapshot.queryParamMap.get('onglet') === 'pieces' ? 'pieces' : 'infos',
  );

  protected readonly dossier = signal<Dossier | null>(null);
  protected readonly checklist = signal<Checklist | null>(null);
  protected readonly erreur = signal('');
  protected readonly message = signal('');
  /** Identifiant de la pièce en cours d'envoi, ou « soumission ». */
  protected readonly occupe = signal<number | 'soumission' | null>(null);

  protected readonly progression = computed(() => {
    const c = this.checklist();
    return c && c.requises > 0 ? Math.round((c.deposees / c.requises) * 100) : 0;
  });

  constructor() {
    this.service.detail(this.reference).subscribe({
      next: (d) => this.dossier.set(d),
      error: (e) => this.erreur.set(e.status === 404 ? 'Dossier introuvable.' : 'Impossible de charger le dossier.'),
    });
    this.rechargerChecklist();
  }

  private rechargerChecklist(): void {
    this.service.checklist(this.reference).subscribe({ next: (c) => this.checklist.set(c), error: () => undefined });
  }

  protected taille(octets: number): string {
    return octets < 1024 * 1024 ? `${Math.max(1, Math.round(octets / 1024))} Ko` : `${(octets / 1024 / 1024).toFixed(1)} Mo`;
  }

  private messageErreur(e: { status?: number; error?: { message?: string } }, defaut: string): string {
    return e.status === 0 ? 'Serveur injoignable : vérifiez que le back-end est démarré.' : (e.error?.message ?? defaut);
  }

  protected choisir(piece: Piece, champ: HTMLInputElement): void {
    const fichier = champ.files?.[0];
    champ.value = ''; // permet de re-sélectionner le même fichier
    if (!fichier) return;
    this.erreur.set('');
    this.message.set('');

    const extension = fichier.name.split('.').pop()?.toLowerCase() ?? '';
    if (!EXTENSIONS.includes(extension)) {
      this.erreur.set('Format non accepté (PDF, JPG ou PNG uniquement).');
      return;
    }
    if (fichier.size > TAILLE_MAX) {
      this.erreur.set('Fichier trop volumineux (5 Mo maximum).');
      return;
    }

    this.occupe.set(piece.id);
    this.service.deposerPiece(this.reference, piece.id, fichier).subscribe({
      next: (c) => { this.checklist.set(c); this.occupe.set(null); },
      error: (e) => { this.erreur.set(this.messageErreur(e, 'Échec du dépôt du fichier.')); this.occupe.set(null); },
    });
  }

  protected voir(piece: Piece): void {
    this.erreur.set('');
    const fenetre = window.open('', '_blank'); // ouverte tout de suite pour ne pas être bloquée
    this.service.fichierPiece(this.reference, piece.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        if (fenetre) fenetre.location.href = url;
        else window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: () => { fenetre?.close(); this.erreur.set("Impossible d'ouvrir le fichier."); },
    });
  }

  protected supprimer(piece: Piece): void {
    if (!confirm(`Supprimer le fichier déposé pour « ${piece.libelle} » ?`)) return;
    this.erreur.set('');
    this.occupe.set(piece.id);
    this.service.supprimerPiece(this.reference, piece.id).subscribe({
      next: (c) => { this.checklist.set(c); this.occupe.set(null); },
      error: (e) => { this.erreur.set(this.messageErreur(e, 'Échec de la suppression.')); this.occupe.set(null); },
    });
  }

  protected soumettre(): void {
    this.erreur.set('');
    this.occupe.set('soumission');
    this.service.soumettre(this.reference).subscribe({
      next: (d) => {
        this.dossier.set(d);
        this.message.set("Dossier soumis à l'analyse.");
        this.occupe.set(null);
        this.rechargerChecklist();
      },
      error: (e) => { this.erreur.set(this.messageErreur(e, 'Échec de la soumission.')); this.occupe.set(null); },
    });
  }
}
