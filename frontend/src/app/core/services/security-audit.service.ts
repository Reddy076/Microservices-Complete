import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogResponse } from '../api/model/auditLogResponse';
import { LoginHistoryResponse } from '../api/model/loginHistoryResponse';
import { SecurityAlertDTO } from '../api/model/securityAlertDTO';
import { SecurityAuditResponse } from '../api/model/securityAuditResponse';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SecurityAuditService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiBaseUrl}/api/security`;

  /** GET /api/security/audit-logs */
  getAuditLogs(): Observable<AuditLogResponse[]> {
    return this.http.get<AuditLogResponse[]>(`${this.BASE}/audit-logs`);
  }

  /** GET /api/security/login-history */
  getLoginHistory(): Observable<LoginHistoryResponse[]> {
    return this.http.get<LoginHistoryResponse[]>(`${this.BASE}/login-history`);
  }

  /** GET /api/security/alerts */
  getSecurityAlerts(): Observable<SecurityAlertDTO[]> {
    return this.http.get<SecurityAlertDTO[]>(`${this.BASE}/alerts`);
  }

  /** PUT /api/security/alerts/{id}/read */
  markAlertAsRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.BASE}/alerts/${id}/read`, {});
  }

  /** DELETE /api/security/alerts/{id} */
  deleteAlert(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/alerts/${id}`);
  }

  /** GET /api/security/audit-report */
  getAuditReport(): Observable<SecurityAuditResponse> {
    return this.http.get<SecurityAuditResponse>(`${this.BASE}/audit-report`);
  }
}
