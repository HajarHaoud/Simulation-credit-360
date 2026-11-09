import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, debounceTime, finalize, of, switchMap } from 'rxjs';
import { DossierService } from '../../core/dossier.service';
import { Dossier, Page, STATUTS, Statut, TypeCredit } from '../../core/models';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { IconComponent } from '../../shared/icon.component';

const TAILLE_PAGE = 8;
const VIDE: Page<Dossier> = { contenu: [], total: 0, page: 1, taille: TAILLE_PAGE, nbPages: 1 };

@Component({
  selector: 'app-dossiers',
  imports: [FormsModule, RouterLink, DatePipe, DecimalPipe, StatusBadgeComponent, IconComponent],
  template: `
    <div class="head">
      <div>
        <h1 class="page-title">Mes dossiers</h1>
        <p class="page-sub">{{ resultat().total }} dossier(s) correspondant à votre recherche.</p>
      </div>
      <a class="btn btn-primary" routerLink="/nouveau-dossier"><app-icon name="plus" /> Nouveau dossier</a>
    </div>

    @if (creeRef) {
      <div class="alert ok">Dossier {{ creeRef }} créé avec le statut « À compléter ».</div>
    }
    @if (erreur()) { <div class="alert warn">{{ erreur() }}</div> }

    <div class="card">
      <div class="filtres">
        <div class="field recherche">
          <label for="q">Recherche</label>
          <input id="q" placeholder="Client ou référence…" [ngModel]="q()" (ngModelChange)="q.set($event); page.set(1)" />
        </div>
        <div class="field">
          <label for="st">Statut</label>
          <select id="st" [ngModel]="statut()" (ngModelChange)="statut.set($event); page.set(1)">
            <option value="">Tous</option>
            @for (s of statuts; track s) { <option [value]="s">{{ s }}</option> }
          </select>
        </div>
        <div class="field">
          <label for="ty">Type de crédit</label>
          <select id="ty" [ngModel]="type()" (ngModelChange)="type.set($event); page.set(1)">
            <option value="">Tous</option>
            @for (t of types(); track t.id) { <option [value]="t.id">{{ t.libelle }}</option> }
          </select>
        </div>
      </div>

      <div class="table-wrap" [class.charge]="chargement()">
        <table class="table">
          <thead><tr><th>Référence</th><th>Client</th><th>Type</th><th>Montant</th><th>Durée</th><th>Statut</th><th>Date MAJ</th></tr></thead>
          <tbody>
            @for (d of resultat().contenu; track d.reference) {
              <tr>
                <td><a class="ref" [routerLink]="['/dossiers', d.reference]">{{ d.reference }}</a></td>
                <td>{{ d.client }}</td>
                <td>{{ d.typeLibelle }}</td>
                <td>{{ d.montant | number: '1.0-0' }} DH</td>
                <td>{{ d.duree }} mois</td>
                <td><app-status-badge [statut]="d.statut" /></td>
                <td>{{ d.dateMaj | date: 'dd/MM/yyyy' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="muted vide">{{ chargement() ? 'Chargement…' : 'Aucun dossier ne correspond à votre recherche.' }}</td></tr>
            }
          </tbody>
        </table>
      </div>

      @if (resultat().nbPages > 1) {
        <div class="pager">
          <button class="btn" [disabled]="page() === 1" (click)="page.set(page() - 1)">Précédent</button>
          <span class="muted">Page {{ page() }} / {{ resultat().nbPages }}</span>
          <button class="btn" [disabled]="page() >= resultat().nbPages" (click)="page.set(page() + 1)">Suivant</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
    .filtres { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
    .recherche { flex: 1; min-width: 220px; }
    .vide { text-align: center; padding: 28px; }
    .charge { opacity: .6; }
    .ref { font-weight: 700; color: var(--orange-dark); text-decoration: none; }
    .ref:hover { text-decoration: underline; }
    .pager { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-top: 14px; }
  `],
})
export class DossiersPage {
  private readonly service = inject(DossierService);
  protected readonly creeRef = inject(ActivatedRoute).snapshot.queryParamMap.get('cree');

  protected readonly statuts = STATUTS;
  protected readonly types = toSignal(this.service.types().pipe(catchError(() => of([] as TypeCredit[]))), {
    initialValue: [] as TypeCredit[],
  });

  protected readonly q = signal('');
  protected readonly statut = signal<Statut | ''>('');
  protected readonly type = signal('');
  protected readonly page = signal(1);

  protected readonly chargement = signal(true);
  protected readonly erreur = signal('');

  private readonly params = computed(() => ({
    q: this.q(), statut: this.statut(), type: this.type(), page: this.page(), taille: TAILLE_PAGE,
  }));

  protected readonly resultat = toSignal(
    toObservable(this.params).pipe(
      debounceTime(250),
      switchMap((p) => {
        this.chargement.set(true);
        this.erreur.set('');
        return this.service.lister(p).pipe(
          catchError(() => {
            this.erreur.set('Impossible de charger les dossiers. Vérifiez que le back-end est démarré.');
            return of(VIDE);
          }),
          finalize(() => this.chargement.set(false)),
        );
      }),
    ),
    { initialValue: VIDE },
  );
}
