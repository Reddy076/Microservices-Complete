import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';
import { ThemeService } from '../../core/services/theme.service';
import { DashboardService } from '../../core/api/api/dashboard.service';

@Component({
  selector: 'app-top-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    NotificationBellComponent
  ],
  templateUrl: './top-header.component.html',
  styleUrl: './top-header.component.css'
})
export class TopHeaderComponent implements OnInit {
  username: string = 'User';
  greeting: string = 'Hello';
  formattedDate: string = '';
  themeService = inject(ThemeService);
  private dashboardService = inject(DashboardService);

  // Vault health pill
  vaultIssues = 0;
  vaultHealthLoaded = false;

  get vaultHealthy(): boolean {
    return this.vaultIssues === 0;
  }

  get vaultStatusLabel(): string {
    if (!this.vaultHealthLoaded) return '';
    if (this.vaultIssues === 0) return 'Vault Healthy';
    return `${this.vaultIssues} issue${this.vaultIssues > 1 ? 's' : ''}`;
  }

  ngOnInit(): void {
    try {
      const token = localStorage.getItem('access_token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        if (payload.sub) {
          this.username = payload.sub;
        }
      }
    } catch {
      // Keep default 'User' if parsing fails
    }

    // Build greeting based on time of day
    const hour = new Date().getHours();
    if (hour < 12) this.greeting = 'Good morning';
    else if (hour < 17) this.greeting = 'Good afternoon';
    else this.greeting = 'Good evening';

    // Format today's date e.g. "Thu, Mar 5, 2026"
    this.formattedDate = new Date().toLocaleDateString('en-US', {
      weekday: 'short', month: 'short', day: 'numeric', year: 'numeric'
    });

    // Fetch vault health for the status pill
    this.loadVaultHealth();
  }

  private loadVaultHealth(): void {
    this.dashboardService.getSecurityScore().subscribe({
      next: (score) => {
        this.vaultIssues = (score.weakPasswords ?? 0)
          + (score.reusedPasswords ?? 0)
          + (score.oldPasswords ?? 0);
        this.vaultHealthLoaded = true;
      },
      error: () => {
        // Silently fail — don't break the header if the API is down
        this.vaultHealthLoaded = false;
      }
    });
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }
}
