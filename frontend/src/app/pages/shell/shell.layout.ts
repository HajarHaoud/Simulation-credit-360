import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  template: `
    <div class="layout">
      <aside class="side">
        <div class="logo">CRÉDIT <span>360°</span></div>
        <nav>
          <a routerLink="/accueil" routerLinkActive="actif"><app-icon name="home" /> Accueil</a>
          <a routerLink="/nouveau-dossier" routerLinkActive="actif"><app-icon name="plus" /> Nouveau dossier</a>
          <a routerLink="/dossiers" routerLinkActive="actif"><app-icon name="folder" /> Mes dossiers</a>
          @if (estAdmin()) {
            <a routerLink="/parametres" routerLinkActive="actif"><app-icon name="sliders" /> Paramètres</a>
          }
        </nav>
        <div class="profil">
          <div class="avatar">{{ initiales() }}</div>
          <div class="qui">
            <strong>{{ user()?.nom }}</strong>
            <small>{{ user()?.fonction }}</small>
          </div>
          <button class="out" (click)="deconnecter()" title="Se déconnecter"><app-icon name="logout" /></button>
        </div>
      </aside>
      <main class="contenu"><router-outlet /></main>
    </div>
  `,
  styles: [`
    .layout { display: grid; grid-template-columns: 240px 1fr; min-height: 100vh; }
    .side { background: var(--nuit); color: #cfd6df; display: flex; flex-direction: column; padding: 18px 12px; position: sticky; top: 0; height: 100vh; }
    .logo { color: #fff; font-weight: 800; font-size: 20px; padding: 6px 12px 22px; }
    .logo span { color: var(--orange); }
    nav { display: flex; flex-direction: column; gap: 4px; flex: 1; }
    nav a { color: inherit; text-decoration: none; padding: 11px 12px; border-radius: 8px; display: flex; align-items: center; gap: 10px; font-weight: 500; }
    nav a:hover { background: var(--nuit-2); }
    nav a.actif { background: var(--orange); color: #fff; }
    .profil { display: flex; align-items: center; gap: 10px; border-top: 1px solid var(--nuit-2); padding: 14px 8px 4px; }
    .avatar { width: 36px; height: 36px; border-radius: 50%; background: var(--nuit-2); color: #fff; display: grid; place-items: center; font-weight: 700; flex-shrink: 0; }
    .qui { display: flex; flex-direction: column; min-width: 0; flex: 1; color: #fff; }
    .qui small { color: #9aa6b4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .out { background: none; border: 0; color: #9aa6b4; cursor: pointer; padding: 6px; border-radius: 6px; }
    .out:hover { color: #fff; background: var(--nuit-2); }
    .contenu { padding: 28px 32px; min-width: 0; }
    @media (max-width: 800px) {
      .layout { grid-template-columns: 1fr; }
      .side { position: static; height: auto; }
      nav { flex-direction: row; flex-wrap: wrap; }
    }
  `],
})
export class ShellLayout {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.user;
  protected readonly estAdmin = computed(() => this.user()?.role === 'admin');
  protected readonly initiales = computed(() =>
    (this.user()?.nom ?? '').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase(),
  );

  protected deconnecter(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
