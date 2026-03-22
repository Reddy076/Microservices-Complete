import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { SecurityAuditService } from '../../../../core/services/security-audit.service';
import { SecurityAuditResponse } from '../../../../core/api/model/securityAuditResponse';

@Component({
  selector: 'app-audit-report',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './audit-report.component.html',
  styleUrl: './audit-report.component.css'
})
export class AuditReportComponent implements OnInit {
  private auditService = inject(SecurityAuditService);

  report: SecurityAuditResponse | null = null;
  isLoading = true;
  errorMessage = '';

  ngOnInit() {
    this.loadReport();
  }

  loadReport() {
    this.isLoading = true;
    this.errorMessage = '';
    this.auditService.getAuditReport().subscribe({
      next: (report) => {
        this.report = report;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load audit report';
        this.isLoading = false;
      }
    });
  }

  getScoreColor(score: number | undefined): string {
    if (!score) return '#9CA3AF';
    if (score >= 80) return '#10B981';
    if (score >= 60) return '#F59E0B';
    if (score >= 40) return '#F97316';
    return '#DC2626';
  }

  getScoreLabel(score: number | undefined): string {
    if (!score) return 'No Data';
    if (score >= 80) return 'Excellent';
    if (score >= 60) return 'Good';
    if (score >= 40) return 'Fair';
    return 'Poor';
  }

  getScoreStrokeDasharray(score: number | undefined): string {
    const s = score || 0;
    const circumference = 339.3;
    const filled = (s / 100) * circumference;
    return `${filled} ${circumference - filled}`;
  }

  getHealthPercentage(count: number | undefined): number {
    if (!count || !this.report?.totalEntries) return 0;
    return Math.round((count / this.report.totalEntries) * 100);
  }
}
