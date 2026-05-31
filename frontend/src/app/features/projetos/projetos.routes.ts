import { Routes } from '@angular/router';

export const projetosRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/projetos-lista/projetos-lista.component').then(m => m.ProjectListComponent),
  },
  {
    path: 'novo',
    loadComponent: () =>
      import('./pages/projeto-form/projeto-form.component').then(m => m.ProjectFormComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/projeto-detalhe/projeto-detalhe.component').then(m => m.ProjectDetailComponent),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/projeto-form/projeto-form.component').then(m => m.ProjectFormComponent),
  },
  {
    path: ':id/environments/novo',
    loadComponent: () =>
      import('./pages/environment-form/environment-form.component').then(m => m.EnvironmentFormComponent),
  },
  {
    path: ':id/environments/:envId/editar',
    loadComponent: () =>
      import('./pages/environment-form/environment-form.component').then(m => m.EnvironmentFormComponent),
  },
  {
    path: ':id/environments/:envId',
    loadComponent: () =>
      import('./pages/environment-detail/environment-detail.component').then(m => m.EnvironmentDetailComponent),
  },
  {
    path: ':id/environments/:envId/items/new',
    loadComponent: () =>
      import('./pages/item-form/item-form.component').then(m => m.ItemFormComponent),
  },
  {
    path: ':id/environments/:envId/items/:itemId/edit',
    loadComponent: () =>
      import('./pages/item-form/item-form.component').then(m => m.ItemFormComponent),
  },
  {
    path: ':id/environments/:envId/items/:itemId/expenses/new',
    loadComponent: () =>
      import('./pages/expense-form/expense-form.component').then(m => m.ExpenseFormComponent),
  },
];
