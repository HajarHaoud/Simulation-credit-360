import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { DossierService } from '../../core/dossier.service';
import { Dossier } from '../../core/models';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-accueil',
  imports: [RouterLink, DatePipe, DecimalPipe, StatusBadgeComponent, IconComponent],
  template: `
    <div class="head">
      <div>
        <h1 class="page-title">Bonjour, {{ prenom() }}</h1>
        <p class="page-sub">Voici l'activité de vos dossiers de crédit.</p>
      </div>
      <a class="btn btn-primary" routerLink="/nouveau-dossier"><app-icon name="plus" /> Nouveau dossier</a>
    </div>

    <section class="grid kpis">
      @for (k of kpis(); track k.label) {
        <div class="card kpi" [class]="'card kpi ' + k.classe">
          <span class="muted">{{ k.label }}</span>
          <strong>{{ k.valeur }}</strong>
        </div>
      }
    </section>

    <section class="card recents">
      <h2>Derniers dossiers mis à jour</h2>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Référence</th><th>Client</th><th>Type</th><th>Montant</th><th>Statut</th><th>Date MAJ</th></tr></thead>
          <tbody>
            @for (d of recents(); track d.reference) {
              <tr>
                <td>{{ d.reference }}</td>
                <td>{{ d.client }}</td>
                <td>{{ d.typeLibelle }}</td>
                <td>{{ d.montant | number: '1.0-0' }} DH</td>
                <td><app-status-badge [statut]="d.statut" /></td>
                <td>{{ d.dateMaj | date: 'dd/MM/yyyy' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="muted">Aucun dossier pour le moment.</td></tr>
            }
          </tbody>
        </table>
      </div>
      <p><a routerLink="/dossiers">Voir tous les dossiers →</a></p>
    </section>
  `,
  styles: [`
    .head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
    .kpis { grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); margin-bottom: 18px; }
    .kpi { display: flex; flex-direction: column; gap: 6px; border-left: 4px solid var(--info); }
    .kpi strong { font-size: 32px; }
    .kpi.ok { border-left-color: var(--ok); }
    .kpi.ko { border-left-color: var(--ko); }
    .kpi.orange { border-left-color: var(--orange); }
    .recents a { color: var(--orange-dark); font-weight: 600; text-decoration: none; }
  `],
})
export class AccueilPage {
  private readonly service = inject(DossierService);
  private readonly auth = inject(AuthService);

  private readonly stats = toSignal(this.service.stats().pipe(catchError(() => of(null))), { initialValue: null });
  protected readonly recents = toSignal(this.service.recents().pipe(catchError(() => of([] as Dossier[]))), {
    initialValue: [] as Dossier[],
  });
  protected readonly prenom = computed(() => (this.auth.user()?.nom ?? '').split(' ')[0]);

  protected readonly kpis = computed(() => {
    const s = this.stats();
    const v = (n: number | undefined) => (n ?? '–');
    return [
      { label: 'Dossiers en cours', valeur: v(s?.enCours), classe: 'orange' },
      { label: 'Dossiers validés', valeur: v(s?.valides), classe: 'ok' },
      { label: 'Impayés', valeur: v(s?.impayes), classe: 'ko' },
      { label: 'Crédits soldés', valeur: v(s?.soldes), classe: '' },
    ];
  });
}
