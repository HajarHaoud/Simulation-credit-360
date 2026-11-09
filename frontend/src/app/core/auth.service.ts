import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { User } from './models';

interface Session { token: string; user: User; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<Session | null>(this.restaurer());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly token = computed(() => this.session()?.token ?? null);

  login(email: string, motDePasse: string): Observable<User> {
    return this.http.post<Session>('/api/auth/login', { email, motDePasse }).pipe(
      tap((s) => {
        this.session.set(s);
        sessionStorage.setItem('credit360.session', JSON.stringify(s));
      }),
      map((s) => s.user),
    );
  }

  logout(): void {
    this.session.set(null);
    sessionStorage.removeItem('credit360.session');
  }

  private restaurer(): Session | null {
    try {
      const brut = sessionStorage.getItem('credit360.session');
      return brut ? (JSON.parse(brut) as Session) : null;
    } catch {
      return null;
    }
  }
}

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.user() ? true : inject(Router).createUrlTree(['/login']);
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.user()?.role === 'admin' ? true : inject(Router).createUrlTree(['/accueil']);
};
