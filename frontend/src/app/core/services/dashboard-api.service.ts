import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SecurityScoreResponse } from '../models/security-score-response.model';
import { PasswordHealthMetricsResponse } from '../models/password-health-metrics-response.model';
import { ReusedPasswordResponse } from '../models/reused-password-response.model';
import { PasswordAgeResponse } from '../models/password-age-response.model';
import { SecurityTrendResponse } from '../models/security-trend-response.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiBaseUrl}/api/dashboard`;

  /** GET /api/dashboard/security-score */
  getSecurityScore(): Observable<SecurityScoreResponse> {
    return this.http.get<SecurityScoreResponse>(`${this.BASE}/security-score`);
  }

  /** GET /api/dashboard/password-health */
  getPasswordHealth(): Observable<PasswordHealthMetricsResponse> {
    return this.http.get<PasswordHealthMetricsResponse>(`${this.BASE}/password-health`);
  }

  /** GET /api/dashboard/reused-passwords */
  getReusedPasswords(): Observable<ReusedPasswordResponse> {
    return this.http.get<ReusedPasswordResponse>(`${this.BASE}/reused-passwords`);
  }

  /** GET /api/dashboard/password-age */
  getPasswordAge(): Observable<PasswordAgeResponse> {
    return this.http.get<PasswordAgeResponse>(`${this.BASE}/password-age`);
  }

  /** GET /api/dashboard/trends?days=30 */
  getSecurityTrends(days: number = 30): Observable<SecurityTrendResponse> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<SecurityTrendResponse>(`${this.BASE}/trends`, { params });
  }

  /** GET /api/dashboard/passwords/weak */
  getWeakPasswordsList(): Observable<import('../api/model/vaultEntryResponse').VaultEntryResponse[]> {
    return this.http.get<import('../api/model/vaultEntryResponse').VaultEntryResponse[]>(`${this.BASE}/passwords/weak`);
  }

  /** GET /api/dashboard/passwords/old */
  getOldPasswordsList(): Observable<import('../api/model/vaultEntryResponse').VaultEntryResponse[]> {
    return this.http.get<import('../api/model/vaultEntryResponse').VaultEntryResponse[]>(`${this.BASE}/passwords/old`);
  }
}
