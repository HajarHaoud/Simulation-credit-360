import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth.service';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.page').then((m) => m.LoginPage) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/shell/shell.layout').then((m) => m.ShellLayout),
    children: [
      { path: 'accueil', loadComponent: () => import('./pages/accueil/accueil.page').then((m) => m.AccueilPage) },
      { path: 'dossiers', loadComponent: () => import('./pages/dossiers/dossiers.page').then((m) => m.DossiersPage) },
      { path: 'dossiers/:reference', loadComponent: () => import('./pages/dossier-detail/dossier-detail.page').then((m) => m.DossierDetailPage) },
      { path: 'nouveau-dossier', loadComponent: () => import('./pages/nouveau-dossier/nouveau-dossier.page').then((m) => m.NouveauDossierPage) },
      { path: 'parametres', canActivate: [adminGuard], loadComponent: () => import('./pages/parametres/parametres.page').then((m) => m.ParametresPage) },
      { path: '', pathMatch: 'full', redirectTo: 'accueil' },
    ],
  },
  { path: '**', redirectTo: '' },
];
