import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { SecurityAuditService } from '../../../../core/services/security-audit.service';
import { LoginHistoryResponse } from '../../../../core/api/model/loginHistoryResponse';

@Component({
  selector: 'app-login-history',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './login-history.component.html',
  styleUrl: './login-history.component.css'
})
export class LoginHistoryComponent implements OnInit {
  private auditService = inject(SecurityAuditService);

  loginHistory: LoginHistoryResponse[] = [];
  isLoading = true;
  errorMessage = '';

  ngOnInit() {
    this.loadLoginHistory();
  }

  loadLoginHistory() {
    this.isLoading = true;
    this.errorMessage = '';
    this.auditService.getLoginHistory().subscribe({
      next: (history) => {
        this.loginHistory = history.sort((a, b) => 
          new Date(b.timestamp || 0).getTime() - new Date(a.timestamp || 0).getTime()
        );
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load login history';
        this.isLoading = false;
      }
    });
  }

  formatDate(timestamp: string | undefined): string {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString();
  }

  getDeviceIcon(deviceInfo: string | undefined): string {
    if (!deviceInfo) return 'monitor';
    const lower = deviceInfo.toLowerCase();
    if (lower.includes('mobile') || lower.includes('phone')) return 'smartphone';
    if (lower.includes('tablet') || lower.includes('ipad')) return 'tablet';
    if (lower.includes('mac')) return 'laptop';
    return 'monitor';
  }

  isSuspicious(login: LoginHistoryResponse): boolean {
    return !login.successful || false;
  }
}
