import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'projects',
    loadChildren: () => import('./features/projetos/projetos.routes').then(m => m.projetosRoutes),
  },
  {
    path: '',
    redirectTo: 'projects',
    pathMatch: 'full',
  },
];
