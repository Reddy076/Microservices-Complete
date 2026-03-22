import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { SecurityAuditService } from '../../../../core/services/security-audit.service';
import { AuditLogResponse } from '../../../../core/api/model/auditLogResponse';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './audit-logs.component.html',
  styleUrl: './audit-logs.component.css'
})
export class AuditLogsComponent implements OnInit {
  private auditService = inject(SecurityAuditService);

  auditLogs: AuditLogResponse[] = [];
  filteredLogs: AuditLogResponse[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';

  // Filters
  searchQuery = '';
  selectedAction = '';
  actions: string[] = [];

  ngOnInit() {
    this.loadAuditLogs();
  }

  loadAuditLogs() {
    this.isLoading = true;
    this.errorMessage = '';
    this.auditService.getAuditLogs().subscribe({
      next: (logs) => {
        this.auditLogs = logs.sort((a, b) =>
          new Date(b.timestamp || 0).getTime() - new Date(a.timestamp || 0).getTime()
        );
        this.filteredLogs = [...this.auditLogs];
        this.extractActions();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load audit logs';
        this.isLoading = false;
      }
    });
  }

  extractActions() {
    const actionSet = new Set<string>();
    this.auditLogs.forEach(log => {
      if (log.action) actionSet.add(log.action);
    });
    this.actions = Array.from(actionSet).sort();
  }

  applyFilters() {
    this.filteredLogs = this.auditLogs.filter(log => {
      const matchesSearch = !this.searchQuery ||
        log.action?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        log.details?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        log.ipAddress?.includes(this.searchQuery);

      const matchesAction = !this.selectedAction || log.action === this.selectedAction;

      return matchesSearch && matchesAction;
    });
  }

  clearFilters() {
    this.searchQuery = '';
    this.selectedAction = '';
    this.filteredLogs = [...this.auditLogs];
  }

  formatDate(timestamp: string | undefined): string {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString();
  }

  getActionIcon(action: string | undefined): string {
    if (!action) return 'info';
    const actionMap: Record<string, string> = {
      'CREATE': 'plus',
      'UPDATE': 'pencil',
      'DELETE': 'trash',
      'LOGIN': 'log-in',
      'LOGOUT': 'log-out',
      'EXPORT': 'download',
      'IMPORT': 'upload',
      'SHARE': 'share',
      'UNSHARE': 'x',
      'DASHBOARD_VIEWED': 'layout-dashboard'
    };
    return actionMap[action.toUpperCase()] || 'info';
  }

  exportToCSV() {
    const headers = ['Timestamp', 'Action', 'Details', 'IP Address'];
    const rows = this.filteredLogs.map(log => [
      log.timestamp,
      log.action,
      log.details,
      log.ipAddress
    ]);

    const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-logs-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);

    this.successMessage = 'Audit logs exported successfully';
    setTimeout(() => this.successMessage = '', 3000);
  }
}
