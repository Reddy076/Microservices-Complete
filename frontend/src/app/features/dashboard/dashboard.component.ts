import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';

import { UserControllerService } from '../../core/api/api/userController.service';
import { VaultService } from '../../core/api/api/vault.service';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { DashboardResponse } from '../../core/api/model/dashboardResponse';
import { SecurityScoreResponse } from '../../core/models/security-score-response.model';
import { PasswordHealthMetricsResponse } from '../../core/models/password-health-metrics-response.model';
import { ReusedPasswordResponse } from '../../core/models/reused-password-response.model';
import { PasswordAgeResponse } from '../../core/models/password-age-response.model';
import { SecurityTrendResponse } from '../../core/models/security-trend-response.model';
import { VaultEntryResponse } from '../../core/api/model/vaultEntryResponse';
import { VaultEntryDetailResponse } from '../../core/api/model/vaultEntryDetailResponse';
import { DashboardListModalComponent } from './dashboard-list-modal/dashboard-list-modal.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule, LucideAngularModule, DashboardListModalComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private userControllerService = inject(UserControllerService);
  private vaultService = inject(VaultService);
  private dashboardApiService = inject(DashboardApiService);
  private router = inject(Router);

  username = 'User';

  // Existing simple counts
  dashboardData: DashboardResponse | null = null;
  trashCount: number = 0;
  isLoading = true;
  errorMessage = '';

  // New detailed data from /api/dashboard/*
  scoreData: SecurityScoreResponse | null = null;
  healthData: PasswordHealthMetricsResponse | null = null;
  reusedData: ReusedPasswordResponse | null = null;
  ageData: PasswordAgeResponse | null = null;
  trendsData: SecurityTrendResponse | null = null;

  // Expanded/collapsed state for accordion sections
  reusedExpanded = false;

  // Modal State
  showListModal = false;
  listModalTitle = '';
  listModalDescription = '';
  listModalEntries: VaultEntryResponse[] = [];
  isListModalLoading = false;

  ngOnInit() {
    this.extractUsername();
    this.loadDashboardData();
  }

  private extractUsername() {
    try {
      const token = localStorage.getItem('access_token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.username = payload.sub || 'User';
      }
    } catch {
      // ignore parse errors
    }
  }

  private loadDashboardData() {
    // In duress mode, show empty dashboard
    if (localStorage.getItem('duress_mode') === 'true') {
      this.dashboardData = { vaultCount: 0, favoriteCount: 0, trashCount: 0 } as DashboardResponse;
      this.trashCount = 0;
      this.scoreData = { overallScore: 0, scoreLabel: 'No Data', totalPasswords: 0 } as SecurityScoreResponse;
      this.healthData = { totalPasswords: 0, strongCount: 0, goodCount: 0, fairCount: 0, weakCount: 0, veryWeakCount: 0 } as PasswordHealthMetricsResponse;
      this.reusedData = { totalReusedGroups: 0, totalAffectedEntries: 0, reusedGroups: [] } as ReusedPasswordResponse;
      this.ageData = { totalPasswords: 0, freshCount: 0, agingCount: 0, oldCount: 0, ancientCount: 0, averageAgeInDays: 0 } as PasswordAgeResponse;
      this.trendsData = { trendPoints: [], scoreChange: 0, trendDirection: 'STABLE', periodLabel: '30-day trend' } as SecurityTrendResponse;
      this.isLoading = false;
      return;
    }

    forkJoin({
      dashboard: this.userControllerService.getDashboard(),
      trash: this.vaultService.getTrashCount(),
      score: this.dashboardApiService.getSecurityScore(),
      health: this.dashboardApiService.getPasswordHealth(),
      reused: this.dashboardApiService.getReusedPasswords(),
      age: this.dashboardApiService.getPasswordAge(),
      trends: this.dashboardApiService.getSecurityTrends(30)
    }).subscribe({
      next: (result) => {
        this.dashboardData = result.dashboard;
        this.trashCount = result.trash.count || 0;
        this.scoreData = result.score;
        this.healthData = result.health;
        this.reusedData = result.reused;
        this.ageData = result.age;
        this.trendsData = result.trends;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load dashboard and security analytics.';
        this.isLoading = false;
      }
    });
  }

  /** Real security score from backend */
  get securityScore(): number {
    return this.scoreData?.overallScore ?? 0;
  }

  get scoreLabel(): string {
    return this.scoreData?.scoreLabel ?? 'Loading...';
  }

  get scoreColor(): string {
    const s = this.securityScore;
    if (s >= 80) return '#10B981';
    if (s >= 50) return '#F59E0B';
    return '#EF4444';
  }

  /** SVG donut ring: circumference = 2 * π * 54 ≈ 339.3 */
  get donutDashArray(): string {
    const circumference = 339.3;
    const filled = (this.securityScore / 100) * circumference;
    return `${filled} ${circumference - filled}`;
  }

  /** Password health bar widths as percentages */
  healthBarWidth(count: number): string {
    const total = this.healthData?.totalPasswords || 1;
    return `${Math.round((count / total) * 100)}%`;
  }

  /** Age distribution bar widths as percentages */
  ageBarWidth(count: number): string {
    const total = this.ageData?.totalPasswords || 1;
    return `${Math.round((count / total) * 100)}%`;
  }

  /** SVG polyline points for the trend mini-chart (160x60 viewport) */
  get trendPolylinePoints(): string {
    const points = this.trendsData?.trendPoints;
    if (!points || points.length < 2) return '';
    const W = 160, H = 60;
    const paddingY = 4; // Padding to prevent stroke clipping
    const drawableH = H - (paddingY * 2);

    const scores = points.map(p => p.overallScore);
    const min = Math.min(...scores);
    const max = Math.max(...scores);
    const range = max - min || 1;

    return points.map((p, i) => {
      const x = (i / (points.length - 1)) * W;
      // Scale within the drawable height and offset by paddingY
      const y = paddingY + drawableH - ((p.overallScore - min) / range) * drawableH;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  get trendIcon(): string {
    const dir = this.trendsData?.trendDirection;
    if (dir === 'UP') return 'trending-up';
    if (dir === 'DOWN') return 'trending-down';
    return 'minus';
  }

  get trendColor(): string {
    const dir = this.trendsData?.trendDirection;
    if (dir === 'UP') return '#10B981';
    if (dir === 'DOWN') return '#EF4444';
    return '#6B7280';
  }

  toggleReusedExpanded() {
    this.reusedExpanded = !this.reusedExpanded;
  }

  // ── Modal Handlers ────────────────────────────────────────────────────────

  openWeakPasswordsModal(event: Event) {
    event.preventDefault(); // Prevent router navigation
    event.stopPropagation();

    this.listModalTitle = 'Weak Passwords';
    this.listModalDescription = 'These passwords are easy to guess or breach. You should update them immediately.';
    this.listModalEntries = [];
    this.isListModalLoading = true;
    this.showListModal = true;

    this.dashboardApiService.getWeakPasswordsList().subscribe({
      next: (entries) => {
        this.listModalEntries = entries;
        this.isListModalLoading = false;
      },
      error: () => {
        // Fallback gracefully
        this.listModalEntries = [];
        this.isListModalLoading = false;
      }
    });
  }

  openOldPasswordsModal(event: Event) {
    event.preventDefault(); // Prevent router navigation
    event.stopPropagation();

    this.listModalTitle = 'Old Passwords';
    this.listModalDescription = 'These passwords have not been changed in over 90 days. It is recommended to rotate them periodically.';
    this.listModalEntries = [];
    this.isListModalLoading = true;
    this.showListModal = true;

    this.dashboardApiService.getOldPasswordsList().subscribe({
      next: (entries) => {
        this.listModalEntries = entries;
        this.isListModalLoading = false;
      },
      error: () => {
        this.listModalEntries = [];
        this.isListModalLoading = false;
      }
    });
  }

  openReusedPasswordsModal(event: Event) {
    event.preventDefault(); // Prevent router navigation
    event.stopPropagation();

    this.listModalTitle = 'Reused Passwords';
    this.listModalDescription = 'These passwords are used across multiple accounts. Reusing passwords drastically increases your risk if one account is compromised.';
    this.listModalEntries = [];
    this.isListModalLoading = false;
    this.showListModal = true;

    if (this.reusedData && this.reusedData.reusedGroups) {
      // Map the groups directly into a flat list of entries for the modal
      const flatList: VaultEntryResponse[] = [];
      for (const group of this.reusedData.reusedGroups) {
        for (const entry of group.entries) {
          flatList.push({
            id: entry.entryId,
            title: entry.title,
            username: entry.username,
            websiteUrl: entry.websiteUrl
          } as unknown as VaultEntryResponse);
        }
      }
      this.listModalEntries = flatList;
    }
  }

  closeListModal() {
    this.showListModal = false;
    this.listModalEntries = [];
  }

  onListModalEntryClick(entry: VaultEntryResponse) {
    // Close the list modal and navigate to the vault to open the entry details
    this.closeListModal();

    if (entry.id) {
      this.router.navigate(['/vault'], { queryParams: { openEntryId: entry.id } });
    }
  }
}
