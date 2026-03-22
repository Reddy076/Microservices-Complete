import { Routes } from '@angular/router';

export const backupRoutes: Routes = [
  {
    path: 'export',
    loadComponent: () => import('./export/export.component').then(m => m.ExportComponent)
  },
  {
    path: 'import',
    loadComponent: () => import('./import/import.component').then(m => m.ImportComponent)
  },
  {
    path: 'snapshots',
    loadComponent: () => import('./snapshots/snapshot-list.component').then(m => m.SnapshotListComponent)
  },
  {
    path: '',
    redirectTo: 'export',
    pathMatch: 'full'
  }
];
