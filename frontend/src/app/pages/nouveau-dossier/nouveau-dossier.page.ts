import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DossierService } from '../../core/dossier.service';
import { catchError, of } from 'rxjs';
import { Famille, TypeCredit } from '../../core/models';
import { TYPE_ATTENTE } from '../../core/credit-config';
import { mensualite, tableauAmortissement } from '../../core/simulation';
import { IconComponent } from '../../shared/icon.component';

const ETAPES = ['Client', 'Crédit et simulation', 'Récapitulatif'];

@Component({
  selector: 'app-nouveau-dossier',
  imports: [FormsModule, DecimalPipe, IconComponent],
  template: `
    <h1 class="page-title">Nouveau dossier</h1>
    <p class="page-sub">Renseignez la demande, simulez le crédit en direct, puis soumettez le dossier.</p>

    <ol class="steps">
      @for (e of etapes; track e; let i = $index) {
        <li [class.actif]="etape() === i" [class.fait]="etape() > i">
          <span class="rond">@if (etape() > i) { <app-icon name="check" /> } @else { {{ i + 1 }} }</span>{{ e }}
        </li>
      }
    </ol>

    @switch (etape()) {
      @case (0) {
        <section class="card">
          <h2>Informations du client</h2>
          <div class="grid form-grid">
            <div class="field"><label for="nom">Nom *</label>
              <input id="nom" [(ngModel)]="client.nom" />
              @if (essaye() && !client.nom.trim()) { <span class="err">Champ obligatoire</span> }</div>
            <div class="field"><label for="prenom">Prénom *</label>
              <input id="prenom" [(ngModel)]="client.prenom" />
              @if (essaye() && !client.prenom.trim()) { <span class="err">Champ obligatoire</span> }</div>
            <div class="field"><label for="cin">CIN *</label>
              <input id="cin" [(ngModel)]="client.cin" />
              @if (essaye() && !client.cin.trim()) { <span class="err">Champ obligatoire</span> }</div>
            <div class="field"><label for="tel">Téléphone *</label>
              <input id="tel" type="tel" [(ngModel)]="client.telephone" />
              @if (essaye() && !client.telephone.trim()) { <span class="err">Champ obligatoire</span> }</div>
            <div class="field"><label for="mail">E-mail</label>
              <input id="mail" type="email" [(ngModel)]="client.email" />
              @if (essaye() && client.email && !emailValide()) { <span class="err">Adresse e-mail invalide</span> }</div>
            <div class="field"><label for="prof">Profession</label>
              <input id="prof" [(ngModel)]="client.profession" /></div>
            <div class="field"><label for="adr">Adresse</label>
              <input id="adr" [(ngModel)]="client.adresse" /></div>
            <div class="field"><label for="rev">Revenu mensuel net (DH)</label>
              <input id="rev" type="number" min="0" [ngModel]="revenu()" (ngModelChange)="revenu.set($event)" /></div>
          </div>
        </section>
      }

      @case (1) {
        <div class="grid duo">
          <section class="card">
            <h2>Paramètres du crédit</h2>
            <div class="familles">
              @for (f of familles; track f) {
                <button type="button" class="famille" [class.on]="type().famille === f" (click)="choisirFamille(f)">{{ f }}</button>
              }
            </div>
            <div class="field">
              <label for="type">Type de crédit</label>
              <select id="type" [ngModel]="typeId()" (ngModelChange)="changerType($event)">
                @for (t of typesFamille(); track t.id) { <option [value]="t.id">{{ t.libelle }}</option> }
              </select>
            </div>
            <div class="field">
              <label for="montant">Montant : <strong>{{ montant() | number: '1.0-0' }} DH</strong></label>
              <input id="montant" type="range" [min]="type().montantMin" [max]="type().montantMax"
                     [step]="pasMontant()" [ngModel]="montant()" (ngModelChange)="montant.set(+$event)" />
              <small class="muted">{{ type().montantMin | number: '1.0-0' }} à {{ type().montantMax | number: '1.0-0' }} DH</small>
            </div>
            <div class="field">
              <label for="duree">Durée : <strong>{{ duree() }} mois</strong> ({{ duree() / 12 | number: '1.0-1' }} ans)</label>
              <input id="duree" type="range" [min]="type().dureeMin" [max]="type().dureeMax"
                     step="6" [ngModel]="duree()" (ngModelChange)="duree.set(+$event)" />
              <small class="muted">{{ type().dureeMin }} à {{ type().dureeMax }} mois</small>
            </div>
          </section>

          <section class="card simu">
            <h2>Simulation</h2>
            <div class="mensu">
              <span class="muted">Mensualité estimée</span>
              <strong>{{ mensu() | number: '1.0-0' }} DH</strong>
              <small class="muted">Taux annuel indicatif : {{ type().taux | number: '1.1-2' }} %</small>
            </div>
            <dl>
              <dt>Montant emprunté</dt><dd>{{ montant() | number: '1.0-0' }} DH</dd>
              <dt>Total des intérêts</dt><dd>{{ interets() | number: '1.0-0' }} DH</dd>
              <dt>Coût total du crédit</dt><dd>{{ total() | number: '1.0-0' }} DH</dd>
            </dl>
            @if (endettement() !== null) {
              <div class="alert" [class.warn]="endettement()! > 40" [class.ok]="endettement()! <= 40">
                Taux d'endettement estimé : <strong>{{ endettement() | number: '1.0-1' }} %</strong>
                @if (endettement()! > 40) { (supérieur à 40 %, à examiner) }
              </div>
            }
          </section>
        </div>

        <section class="card" style="margin-top:16px">
          <h2>Tableau d'amortissement</h2>
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>N°</th><th>Mensualité</th><th>Intérêts</th><th>Capital</th><th>Restant dû</th></tr></thead>
              <tbody>
                @for (l of lignesAffichees(); track l.n) {
                  <tr><td>{{ l.n }}</td><td>{{ l.mensualite | number: '1.0-0' }}</td><td>{{ l.interets | number: '1.0-0' }}</td>
                      <td>{{ l.capital | number: '1.0-0' }}</td><td>{{ l.restant | number: '1.0-0' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
          @if (lignes().length > 12) {
            <p><button type="button" class="btn" (click)="toutVoir.set(!toutVoir())">
              {{ toutVoir() ? 'Réduire' : 'Voir les ' + lignes().length + ' échéances' }}</button></p>
          }
        </section>
      }

      @case (2) {
        <section class="card">
          <h2>Récapitulatif</h2>
          <dl class="recap">
            <dt>Client</dt><dd>{{ client.prenom }} {{ client.nom }} ({{ client.cin }})</dd>
            <dt>Contact</dt><dd>{{ client.telephone }}{{ client.email ? ' · ' + client.email : '' }}</dd>
            <dt>Type de crédit</dt><dd>{{ type().libelle }} ({{ type().famille }})</dd>
            <dt>Montant</dt><dd>{{ montant() | number: '1.0-0' }} DH sur {{ duree() }} mois</dd>
            <dt>Mensualité estimée</dt><dd>{{ mensu() | number: '1.0-0' }} DH</dd>
          </dl>
          <p class="muted">À la soumission, une référence est générée et le dossier passe au statut « À compléter » : vous pourrez ensuite déposer les pièces justificatives.</p>
        </section>
      }
    }

    @if (erreurServeur()) { <div class="alert warn" style="margin-top:14px">{{ erreurServeur() }}</div> }
    <div class="actions">
      <button class="btn" type="button" [disabled]="etape() === 0" (click)="precedent()">Précédent</button>
      @if (etape() < 2) {
        <button class="btn btn-primary" type="button" (click)="suivant()">Suivant</button>
      } @else {
        <button class="btn btn-primary" type="button" [disabled]="envoi()" (click)="soumettre()">{{ envoi() ? 'Envoi…' : 'Soumettre le dossier' }}</button>
      }
    </div>
  `,
  styles: [`
    .steps { list-style: none; display: flex; gap: 8px; padding: 0; margin: 0 0 18px; flex-wrap: wrap; }
    .steps li { display: flex; align-items: center; gap: 8px; padding: 8px 14px; background: #fff; border: 1px solid var(--bord); border-radius: 999px; color: var(--muted); font-weight: 600; }
    .steps .rond { width: 22px; height: 22px; border-radius: 50%; background: var(--bord); display: grid; place-items: center; font-size: 12px; color: var(--texte); }
    .steps .actif { color: var(--texte); border-color: var(--orange); }
    .steps .actif .rond { background: var(--orange); color: #fff; }
    .steps .fait .rond { background: var(--ok); color: #fff; }
    .form-grid { grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); }
    .duo { grid-template-columns: 1.2fr 1fr; }
    .duo .card { display: flex; flex-direction: column; gap: 14px; }
    .familles { display: flex; gap: 8px; }
    .famille { flex: 1; padding: 10px; border: 1px solid var(--bord); background: #fff; border-radius: 8px; font: inherit; font-weight: 600; cursor: pointer; }
    .famille.on { border-color: var(--orange); background: var(--orange-soft); color: var(--orange-dark); }
    input[type=range] { padding: 0; accent-color: var(--orange); }
    .mensu { display: flex; flex-direction: column; background: var(--orange-soft); border-radius: 10px; padding: 14px; }
    .mensu strong { font-size: 34px; color: var(--orange-dark); }
    dl { display: grid; grid-template-columns: 1fr auto; gap: 8px 16px; margin: 0; }
    dt { color: var(--muted); } dd { margin: 0; font-weight: 600; text-align: right; }
    .recap { grid-template-columns: 180px 1fr; } .recap dd { text-align: left; }
    .actions { display: flex; justify-content: space-between; margin-top: 18px; }
    @media (max-width: 900px) { .duo { grid-template-columns: 1fr; } }
  `],
})
export class NouveauDossierPage {
  private readonly service = inject(DossierService);
  private readonly router = inject(Router);

  protected readonly etapes = ETAPES;
  protected readonly familles: Famille[] = ['Consommation', 'Immobilier'];

  protected readonly etape = signal(0);
  protected readonly essaye = signal(false);
  protected readonly toutVoir = signal(false);
  protected readonly envoi = signal(false);
  protected readonly erreurServeur = signal('');

  protected client = { nom: '', prenom: '', cin: '', telephone: '', email: '', profession: '', adresse: '' };
  protected readonly revenu = signal<number | null>(null);

  protected readonly typeId = signal('personnel');
  protected readonly types = toSignal(this.service.types().pipe(catchError(() => of([] as TypeCredit[]))), {
    initialValue: [] as TypeCredit[],
  });
  protected readonly type = computed(
    () => this.types().find((t) => t.id === this.typeId()) ?? this.types()[0] ?? TYPE_ATTENTE,
  );
  protected readonly montant = signal(80000);
  protected readonly duree = signal(60);

  protected readonly typesFamille = computed(() => this.types().filter((t) => t.famille === this.type().famille));
  protected readonly pasMontant = computed(() => (this.type().famille === 'Immobilier' ? 10000 : 1000));

  protected readonly mensu = computed(() => mensualite(this.montant(), this.type().taux, this.duree()));
  protected readonly total = computed(() => this.mensu() * this.duree());
  protected readonly interets = computed(() => this.total() - this.montant());
  protected readonly lignes = computed(() => tableauAmortissement(this.montant(), this.type().taux, this.duree()));
  protected readonly lignesAffichees = computed(() => (this.toutVoir() ? this.lignes() : this.lignes().slice(0, 12)));
  protected readonly endettement = computed(() => {
    const r = this.revenu();
    return r && r > 0 ? (this.mensu() / r) * 100 : null;
  });

  protected emailValide(): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.client.email);
  }

  private etape1Valide(): boolean {
    const c = this.client;
    return !!(c.nom.trim() && c.prenom.trim() && c.cin.trim() && c.telephone.trim()) && (!c.email || this.emailValide());
  }

  protected choisirFamille(f: Famille): void {
    const premier = this.types().find((t) => t.famille === f);
    if (premier) this.changerType(premier.id);
  }

  /** Recale montant et durée dans les bornes du type choisi. */
  protected changerType(id: string): void {
    this.typeId.set(id);
    const t = this.types().find((x) => x.id === id) ?? TYPE_ATTENTE;
    this.montant.set(Math.min(Math.max(this.montant(), t.montantMin), t.montantMax));
    this.duree.set(Math.min(Math.max(this.duree(), t.dureeMin), t.dureeMax));
    this.toutVoir.set(false);
  }

  protected suivant(): void {
    if (this.etape() === 0) {
      this.essaye.set(true);
      if (!this.etape1Valide()) return;
    }
    this.etape.update((e) => e + 1);
  }

  protected precedent(): void {
    this.etape.update((e) => Math.max(0, e - 1));
  }

  protected soumettre(): void {
    const c = this.client;
    const ouNull = (s: string) => (s.trim() ? s.trim() : null);
    this.envoi.set(true);
    this.erreurServeur.set('');
    this.service
      .creer({
        nom: c.nom.trim(), prenom: c.prenom.trim(), cin: c.cin.trim(), telephone: c.telephone.trim(),
        email: ouNull(c.email), profession: ouNull(c.profession), adresse: ouNull(c.adresse),
        revenuMensuel: this.revenu(), typeId: this.typeId(), montant: this.montant(), duree: this.duree(),
      })
      .subscribe({
        next: (d) => this.router.navigate(['/dossiers', d.reference], { queryParams: { cree: d.reference, onglet: 'pieces' } }),
        error: (e) => {
          this.envoi.set(false);
          this.erreurServeur.set(e.status === 0 ? 'Serveur injoignable : vérifiez que le back-end est démarré.' : (e.error?.message ?? 'Erreur lors de la création du dossier.'));
        },
      });
  }
}
