import { Component, computed, input } from '@angular/core';
import { Statut } from '../core/models';

const CLASSES: Record<Statut, string> = {
  'À compléter': 'warn', 'En analyse': 'info', 'En cours': 'info', 'Validé': 'ok',
  'Refusé': 'ko', 'Signé': 'ok', 'Impayé': 'ko', 'Soldé': 'neutral',
};

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="classe()">{{ statut() }}</span>`,
})
export class StatusBadgeComponent {
  readonly statut = input.required<Statut>();
  protected readonly classe = computed(() => 'badge ' + CLASSES[this.statut()]);
}
