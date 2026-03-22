import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { SecurityAuditService } from '../../../../core/services/security-audit.service';
import { SecurityAlertDTO } from '../../../../core/api/model/securityAlertDTO';

@Component({
  selector: 'app-security-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './security-alerts.component.html',
  styleUrl: './security-alerts.component.css'
})
export class SecurityAlertsComponent implements OnInit {
  private auditService = inject(SecurityAuditService);

  alerts: SecurityAlertDTO[] = [];
  filteredAlerts: SecurityAlertDTO[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';

  // Filters
  filterStatus: 'all' | 'unread' | 'read' = 'all';
  filterSeverity: 'all' | 'high' | 'medium' | 'low' = 'all';

  ngOnInit() {
    this.loadAlerts();
  }

  loadAlerts() {
    this.isLoading = true;
    this.errorMessage = '';
    this.auditService.getSecurityAlerts().subscribe({
      next: (alerts) => {
        this.alerts = alerts.sort((a, b) => 
          new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        this.applyFilters();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load security alerts';
        this.isLoading = false;
      }
    });
  }

  applyFilters() {
    this.filteredAlerts = this.alerts.filter(alert => {
      const matchesStatus = this.filterStatus === 'all' || 
        (this.filterStatus === 'unread' && !alert.read) ||
        (this.filterStatus === 'read' && alert.read);
      
      const matchesSeverity = this.filterSeverity === 'all' || 
        alert.severity?.toLowerCase() === this.filterSeverity;
      
      return matchesStatus && matchesSeverity;
    });
  }

  markAsRead(id: number | undefined) {
    if (!id) return;
    this.auditService.markAlertAsRead(id).subscribe({
      next: () => {
        const alert = this.alerts.find(a => a.id === id);
        if (alert) {
          alert.read = true;
          this.applyFilters();
        }
        this.successMessage = 'Alert marked as read';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.errorMessage = 'Failed to mark alert as read';
      }
    });
  }

  deleteAlert(id: number | undefined) {
    if (!id) return;
    this.auditService.deleteAlert(id).subscribe({
      next: () => {
        this.alerts = this.alerts.filter(a => a.id !== id);
        this.applyFilters();
        this.successMessage = 'Alert deleted';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.errorMessage = 'Failed to delete alert';
      }
    });
  }

  markAllAsRead() {
    const unreadAlerts = this.alerts.filter(a => !a.read);
    let completed = 0;
    unreadAlerts.forEach(alert => {
      if (alert.id) {
        this.auditService.markAlertAsRead(alert.id).subscribe({
          next: () => {
            alert.read = true;
            completed++;
            if (completed === unreadAlerts.length) {
              this.applyFilters();
              this.successMessage = 'All alerts marked as read';
              setTimeout(() => this.successMessage = '', 3000);
            }
          }
        });
      }
    });
  }

  getSeverityIcon(severity: string | undefined): string {
    switch (severity?.toLowerCase()) {
      case 'high': return 'alert-triangle';
      case 'medium': return 'alert-circle';
      case 'low': return 'info';
      default: return 'bell';
    }
  }

  getSeverityClass(severity: string | undefined): string {
    switch (severity?.toLowerCase()) {
      case 'high': return 'severity-high';
      case 'medium': return 'severity-medium';
      case 'low': return 'severity-low';
      default: return '';
    }
  }

  formatDate(timestamp: string | undefined): string {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString();
  }

  getUnreadCount(): number {
    return this.alerts.filter(a => !a.read).length;
  }
}
