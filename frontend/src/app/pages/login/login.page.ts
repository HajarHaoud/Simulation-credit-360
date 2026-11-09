import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="wrap">
      <aside class="hero">
        <div class="brand">CRÉDIT <span>360°</span></div>
        <p>Un parcours digital, du projet à la réussite.</p>
        <small>Plus simple · Plus rapide · Plus proche de nos clients</small>
      </aside>
      <main class="panel">
        <form class="card form" (ngSubmit)="connecter()">
          <h1>Connexion</h1>
          <p class="muted">Accédez à votre espace conseiller.</p>
          @if (erreur()) { <div class="alert warn">{{ erreur() }}</div> }
          <div class="field">
            <label for="email">Adresse e-mail</label>
            <input id="email" name="email" type="email" [(ngModel)]="email" required autocomplete="username" />
          </div>
          <div class="field">
            <label for="mdp">Mot de passe</label>
            <input id="mdp" name="mdp" type="password" [(ngModel)]="motDePasse" required autocomplete="current-password" />
          </div>
          <button class="btn btn-primary" type="submit">Se connecter</button>
          <div class="demo">
            <span class="muted">Comptes de démonstration (mot de passe : demo1234)</span>
            <div class="chips">
              @for (c of comptes; track c.role) {
                <button type="button" class="chip" (click)="remplir(c.email)">{{ c.role }}</button>
              }
            </div>
          </div>
        </form>
      </main>
    </div>
  `,
  styles: [`
    .wrap { display: grid; grid-template-columns: 1.1fr 1fr; min-height: 100vh; }
    .hero { background: linear-gradient(160deg, var(--nuit), var(--nuit-2)); color: #fff; padding: 64px; display: flex; flex-direction: column; justify-content: center; gap: 14px; }
    .brand { font-size: 44px; font-weight: 800; letter-spacing: .02em; }
    .brand span { color: var(--orange); }
    .hero p { font-size: 20px; margin: 0; opacity: .9; }
    .hero small { opacity: .6; }
    .panel { display: flex; align-items: center; justify-content: center; padding: 24px; }
    .form { width: 100%; max-width: 400px; display: flex; flex-direction: column; gap: 14px; padding: 28px; }
    .form h1 { font-size: 22px; }
    .demo { border-top: 1px solid var(--bord); padding-top: 12px; display: flex; flex-direction: column; gap: 8px; font-size: 12.5px; }
    .chips { display: flex; gap: 6px; flex-wrap: wrap; }
    .chip { border: 1px solid var(--bord); background: #fff; border-radius: 999px; padding: 4px 12px; cursor: pointer; font: inherit; text-transform: capitalize; }
    .chip:hover { border-color: var(--orange); color: var(--orange); }
    @media (max-width: 800px) { .wrap { grid-template-columns: 1fr; } .hero { padding: 32px; } }
  `],
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly comptes = [
    { role: 'conseiller', email: 'conseiller@credit360.ma' },
    { role: 'analyste', email: 'analyste@credit360.ma' },
    { role: 'responsable', email: 'responsable@credit360.ma' },
    { role: 'admin', email: 'admin@credit360.ma' },
  ];
  protected email = '';
  protected motDePasse = '';
  protected readonly erreur = signal('');

  protected remplir(email: string): void {
    this.email = email;
    this.motDePasse = 'demo1234';
  }

  protected connecter(): void {
    this.erreur.set('');
    this.auth.login(this.email, this.motDePasse).subscribe({
      next: () => this.router.navigateByUrl('/accueil'),
      error: (e) =>
        this.erreur.set(e.status === 0 ? 'Serveur injoignable : vérifiez que le back-end est démarré.' : (e.error?.message ?? 'Identifiants incorrects.')),
    });
  }
}
