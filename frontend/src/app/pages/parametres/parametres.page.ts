import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { catchError, of } from 'rxjs';
import { DossierService } from '../../core/dossier.service';
import { TypeCredit, User } from '../../core/models';

@Component({
  selector: 'app-parametres',
  imports: [DecimalPipe],
  template: `
    <h1 class="page-title">Paramètres</h1>
    <p class="page-sub">Aperçu en lecture seule. La modification des utilisateurs, types de crédit, pièces et seuils sera ajoutée à l'étape 6.</p>

    <div class="card">
      <h2>Utilisateurs</h2>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Nom</th><th>Fonction</th><th>E-mail</th><th>Rôle</th></tr></thead>
          <tbody>
            @for (u of utilisateurs(); track u.id) {
              <tr><td>{{ u.nom }}</td><td>{{ u.fonction }}</td><td>{{ u.email }}</td><td>{{ u.role }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h2>Types de crédit (valeurs fictives)</h2>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Type</th><th>Famille</th><th>Taux annuel</th><th>Montant</th><th>Durée</th></tr></thead>
          <tbody>
            @for (t of types(); track t.id) {
              <tr>
                <td>{{ t.libelle }}</td>
                <td>{{ t.famille }}</td>
                <td>{{ t.taux | number: '1.1-2' }} %</td>
                <td>{{ t.montantMin | number: '1.0-0' }} à {{ t.montantMax | number: '1.0-0' }} DH</td>
                <td>{{ t.dureeMin }} à {{ t.dureeMax }} mois</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class ParametresPage {
  private readonly service = inject(DossierService);
  protected readonly types = toSignal(this.service.types().pipe(catchError(() => of([] as TypeCredit[]))), {
    initialValue: [] as TypeCredit[],
  });
  protected readonly utilisateurs = toSignal(this.service.utilisateurs().pipe(catchError(() => of([] as User[]))), {
    initialValue: [] as User[],
  });
}
