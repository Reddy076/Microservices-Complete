import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { LayoutComponent } from './layout/layout.component';

export const routes: Routes = [
  // Public routes (full-screen, no shell)
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'share/:token',
    loadComponent: () => import('./features/secure-sharing/share-access/share-access.component').then(m => m.ShareAccessComponent)
  },

  // Authenticated routes (inside LayoutComponent shell)
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'vault',
        loadComponent: () => import('./features/vault/vault.component').then(m => m.VaultComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/user-profile/settings-layout/settings-layout.component').then(m => m.SettingsLayoutComponent),
        children: [
          {
            path: 'account',
            loadComponent: () => import('./features/user-profile/settings-layout/account-settings/account-settings.component').then(m => m.AccountSettingsComponent)
          },
          {
            path: 'security',
            loadComponent: () => import('./features/user-profile/settings-layout/security-settings/security-settings.component').then(m => m.SecuritySettingsComponent)
          },
          {
            path: 'preferences',
            loadComponent: () => import('./features/user-profile/settings-layout/preferences-settings/preferences-settings.component').then(m => m.PreferencesSettingsComponent)
          },
          {
            path: 'sessions',
            loadComponent: () => import('./features/user-profile/settings-layout/sessions-settings/sessions-settings.component').then(m => m.SessionsSettingsComponent)
          },
          {
            path: 'audit-logs',
            loadComponent: () => import('./features/user-profile/settings-layout/audit-logs/audit-logs.component').then(m => m.AuditLogsComponent)
          },
          {
            path: 'security-alerts',
            loadComponent: () => import('./features/user-profile/settings-layout/security-alerts/security-alerts.component').then(m => m.SecurityAlertsComponent)
          },
          {
            path: '',
            redirectTo: 'account',
            pathMatch: 'full'
          }
        ]
      },
      {
        path: 'backup',
        loadChildren: () => import('./features/backup/backup.routes').then(m => m.backupRoutes)
      },
      {
        path: 'ai-password-analyzer',
        loadComponent: () => import('./features/ai/password-analyzer/password-analyzer.component').then(m => m.PasswordAnalyzerComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  // Wildcard
  {
    path: '**',
    redirectTo: 'login'
  }
];
