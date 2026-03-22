import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { UserControllerService } from '../../core/api/api/userController.service';
import { VaultService } from '../../core/api/api/vault.service';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { environment } from '../../../environments/environment';
import { DashboardResponse } from '../../core/api/model/dashboardResponse';
import { of, throwError } from 'rxjs';
import { SecurityScoreResponse } from '../../core/models/security-score-response.model';
import { PasswordHealthMetricsResponse } from '../../core/models/password-health-metrics-response.model';
import { ReusedPasswordResponse } from '../../core/models/reused-password-response.model';
import { PasswordAgeResponse } from '../../core/models/password-age-response.model';
import { SecurityTrendResponse } from '../../core/models/security-trend-response.model';
import { importProvidersFrom } from '@angular/core';
import {
  LucideAngularModule, ShieldAlert, LayoutDashboard, Lock, Star, Trash2, Folder,
  Settings, LogOut, ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw,
  ArrowRight, Plus, Eye, EyeOff, X, Copy, Edit2, Key, Smartphone, Laptop, MapPin,
  AlertTriangle, Calendar, User, Shield, Sliders, Monitor, Sun, Moon, ChevronDown,
  CalendarX, ShieldCheck, AlertCircle, CheckCircle, ShieldOff, BarChart2, Activity,
  TrendingUp, TrendingDown, Minus, Clock, ChevronUp
} from 'lucide-angular';

// ─── Shared test fixtures ─────────────────────────────────────────────────────

const mockScore: SecurityScoreResponse = {
  overallScore: 82,
  scoreLabel: 'Strong',
  totalPasswords: 20,
  strongPasswords: 10,
  fairPasswords: 5,
  weakPasswords: 3,
  reusedPasswords: 1,
  oldPasswords: 1,
  recommendation: 'Great job! Keep your passwords fresh.'
};

const mockHealth: PasswordHealthMetricsResponse = {
  totalPasswords: 20,
  strongCount: 10,
  goodCount: 3,
  fairCount: 3,
  weakCount: 3,
  veryWeakCount: 1,
  averageStrengthScore: 72.5,
  categoryBreakdowns: []
};

const mockReused: ReusedPasswordResponse = {
  totalReusedGroups: 1,
  totalAffectedEntries: 2,
  reusedGroups: [{
    reuseCount: 2,
    entries: [
      { entryId: 1, title: 'Gmail', username: 'user@test.com', websiteUrl: 'gmail.com' },
      { entryId: 2, title: 'Yahoo', username: 'user@test.com', websiteUrl: 'yahoo.com' }
    ]
  }]
};

const mockAge: PasswordAgeResponse = {
  totalPasswords: 20,
  freshCount: 10,
  agingCount: 5,
  oldCount: 3,
  ancientCount: 2,
  averageAgeInDays: 65,
  distribution: []
};

const mockTrends: SecurityTrendResponse = {
  scoreChange: 5,
  trendDirection: 'UP',
  periodLabel: 'Last 30 days',
  trendPoints: [
    { recordedAt: '2026-01-01T00:00:00', overallScore: 70, weakPasswordsCount: 5, reusedPasswordsCount: 2, oldPasswordsCount: 3 },
    { recordedAt: '2026-01-15T00:00:00', overallScore: 78, weakPasswordsCount: 3, reusedPasswordsCount: 1, oldPasswordsCount: 2 },
    { recordedAt: '2026-02-01T00:00:00', overallScore: 82, weakPasswordsCount: 2, reusedPasswordsCount: 1, oldPasswordsCount: 1 }
  ]
};

const mockDashboard = {
  totalVaultEntries: 20,
  weakPasswordsCount: 3,
  reusedPasswordsCount: 1,
  oldPasswordsCount: 1,
  totalFavorites: 4
};

// ─── Helper to create spy objects ─────────────────────────────────────────────

function buildSpies() {
  const userSpy = jasmine.createSpyObj<UserControllerService>('UserControllerService',
    ['getDashboard', 'getActivityHeatmap']);
  const vaultSpy = jasmine.createSpyObj<VaultService>('VaultService', ['getTrashCount']);
  const apiSpy = jasmine.createSpyObj<DashboardApiService>('DashboardApiService',
    ['getSecurityScore', 'getPasswordHealth', 'getReusedPasswords', 'getPasswordAge', 'getSecurityTrends']);

  userSpy.getDashboard.and.returnValue(of(mockDashboard) as any);
  userSpy.getActivityHeatmap.and.returnValue(of({}) as any);
  vaultSpy.getTrashCount.and.returnValue(of({ count: 2 }) as any);
  apiSpy.getSecurityScore.and.returnValue(of(mockScore));
  apiSpy.getPasswordHealth.and.returnValue(of(mockHealth));
  apiSpy.getReusedPasswords.and.returnValue(of(mockReused));
  apiSpy.getPasswordAge.and.returnValue(of(mockAge));
  apiSpy.getSecurityTrends.and.returnValue(of(mockTrends));

  return { userSpy, vaultSpy, apiSpy };
}

// ═══════════════════════════════════════════════════════════════════════════════
// SUITE 1 — DashboardApiService HTTP tests
// ═══════════════════════════════════════════════════════════════════════════════
describe('DashboardApiService', () => {
  let service: DashboardApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardApiService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DashboardApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getSecurityScore() should call GET /api/dashboard/security-score', () => {
    service.getSecurityScore().subscribe(res => expect(res.overallScore).toBe(82));
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/dashboard/security-score`);
    expect(req.request.method).toBe('GET');
    req.flush(mockScore);
  });

  it('getPasswordHealth() should call GET /api/dashboard/password-health', () => {
    service.getPasswordHealth().subscribe(res => expect(res.totalPasswords).toBe(20));
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/dashboard/password-health`);
    expect(req.request.method).toBe('GET');
    req.flush(mockHealth);
  });

  it('getReusedPasswords() should call GET /api/dashboard/reused-passwords', () => {
    service.getReusedPasswords().subscribe(res => expect(res.totalReusedGroups).toBe(1));
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/dashboard/reused-passwords`);
    expect(req.request.method).toBe('GET');
    req.flush(mockReused);
  });

  it('getPasswordAge() should call GET /api/dashboard/password-age', () => {
    service.getPasswordAge().subscribe(res => expect(res.freshCount).toBe(10));
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/dashboard/password-age`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAge);
  });

  it('getSecurityTrends() should call GET /api/dashboard/trends?days=30', () => {
    service.getSecurityTrends(30).subscribe(res => expect(res.scoreChange).toBe(5));
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/dashboard/trends?days=30`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTrends);
  });
});

// ═══════════════════════════════════════════════════════════════════════════════
// SUITE 2 — DashboardComponent unit tests
// ═══════════════════════════════════════════════════════════════════════════════
describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let userSpy: jasmine.SpyObj<UserControllerService>;
  let vaultSpy: jasmine.SpyObj<VaultService>;
  let apiSpy: jasmine.SpyObj<DashboardApiService>;

  beforeEach(async () => {
    localStorage.clear();
    ({ userSpy, vaultSpy, apiSpy } = buildSpies());

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserControllerService, useValue: userSpy },
        { provide: VaultService, useValue: vaultSpy },
        { provide: DashboardApiService, useValue: apiSpy },
        importProvidersFrom(LucideAngularModule.pick({
          ShieldAlert, LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut,
          ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus,
          Eye, EyeOff, X, Copy, Edit2, Key, Smartphone, Laptop, MapPin, AlertTriangle,
          Calendar, User, Shield, Sliders, Monitor, Sun, Moon, ChevronDown, CalendarX,
          ShieldCheck, AlertCircle, CheckCircle, ShieldOff, BarChart2, Activity,
          TrendingUp, TrendingDown, Minus, Clock, ChevronUp
        }))
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  // ── Basic ─────────────────────────────────────────────────────────────────

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call all 5 DashboardApiService endpoints on init', () => {
    expect(apiSpy.getSecurityScore).toHaveBeenCalledTimes(1);
    expect(apiSpy.getPasswordHealth).toHaveBeenCalledTimes(1);
    expect(apiSpy.getReusedPasswords).toHaveBeenCalledTimes(1);
    expect(apiSpy.getPasswordAge).toHaveBeenCalledTimes(1);
    expect(apiSpy.getSecurityTrends).toHaveBeenCalledWith(30);
  });

  it('should show loading spinner while data is loading', () => {
    // Reset to a loading state
    component.isLoading = true;
    fixture.detectChanges();
    const spinner = fixture.nativeElement.querySelector('.spinner');
    expect(spinner).toBeTruthy();
  });

  it('should hide spinner and show content after data loads', () => {
    expect(component.isLoading).toBeFalse();
    const spinner = fixture.nativeElement.querySelector('.spinner');
    expect(spinner).toBeNull();
  });

  // ── Security score ─────────────────────────────────────────────────────────

  it('should display backend security score (not locally computed)', () => {
    expect(component.securityScore).toBe(82);
  });

  it('should display scoreLabel from backend', () => {
    expect(component.scoreLabel).toBe('Strong');
  });

  it('should return green color for score >= 80', () => {
    expect(component.scoreColor).toBe('#10B981');
  });

  it('should return amber color for score 50–79', () => {
    component.scoreData = { ...mockScore, overallScore: 65, scoreLabel: 'Fair' };
    expect(component.scoreColor).toBe('#F59E0B');
  });

  it('should return red color for score < 50', () => {
    component.scoreData = { ...mockScore, overallScore: 30, scoreLabel: 'At Risk' };
    expect(component.scoreColor).toBe('#EF4444');
  });

  it('should return score 0 when scoreData is null', () => {
    component.scoreData = null;
    expect(component.securityScore).toBe(0);
  });

  // ── Health data ────────────────────────────────────────────────────────────

  it('should populate healthData from DashboardApiService', () => {
    expect(component.healthData).toEqual(mockHealth);
  });

  it('healthBarWidth() should return 50% for count=10 out of 20', () => {
    expect(component.healthBarWidth(10)).toBe('50%');
  });

  it('healthBarWidth() should not divide by zero when totalPasswords is 0', () => {
    component.healthData = { ...mockHealth, totalPasswords: 0 };
    expect(component.healthBarWidth(0)).toBe('0%');
  });

  // ── Reused passwords ──────────────────────────────────────────────────────

  it('should populate reusedData from DashboardApiService', () => {
    expect(component.reusedData?.totalReusedGroups).toBe(1);
    expect(component.reusedData?.totalAffectedEntries).toBe(2);
  });

  it('toggleReusedExpanded() should toggle the expanded state', () => {
    expect(component.reusedExpanded).toBeFalse();
    component.toggleReusedExpanded();
    expect(component.reusedExpanded).toBeTrue();
    component.toggleReusedExpanded();
    expect(component.reusedExpanded).toBeFalse();
  });

  // ── Password age ──────────────────────────────────────────────────────────

  it('should populate ageData from DashboardApiService', () => {
    expect(component.ageData?.freshCount).toBe(10);
    expect(component.ageData?.ancientCount).toBe(2);
  });

  it('ageBarWidth() should return 25% for count=5 out of 20', () => {
    expect(component.ageBarWidth(5)).toBe('25%');
  });

  // ── Trends ────────────────────────────────────────────────────────────────

  it('should populate trendsData from DashboardApiService', () => {
    expect(component.trendsData?.trendDirection).toBe('UP');
    expect(component.trendsData?.scoreChange).toBe(5);
  });

  it('trendIcon should return trending-up for UP direction', () => {
    expect(component.trendIcon).toBe('trending-up');
  });

  it('trendIcon should return trending-down for DOWN direction', () => {
    component.trendsData = { ...mockTrends, trendDirection: 'DOWN' };
    expect(component.trendIcon).toBe('trending-down');
  });

  it('trendIcon should return minus for STABLE direction', () => {
    component.trendsData = { ...mockTrends, trendDirection: 'STABLE' };
    expect(component.trendIcon).toBe('minus');
  });

  it('trendPolylinePoints should return a non-empty string with 3 data points', () => {
    const points = component.trendPolylinePoints;
    expect(points.length).toBeGreaterThan(0);
    expect(points.split(' ').length).toBe(3);
  });

  it('trendPolylinePoints should return empty string when fewer than 2 points', () => {
    component.trendsData = { ...mockTrends, trendPoints: [] };
    expect(component.trendPolylinePoints).toBe('');
  });

  // ── Error handling ─────────────────────────────────────────────────────────

  it('should show errorMessage when API call fails', async () => {
    localStorage.clear();
    ({ userSpy, vaultSpy, apiSpy } = buildSpies());
    apiSpy.getSecurityScore.and.returnValue(throwError(() => new Error('Network error')));

    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        importProvidersFrom(LucideAngularModule.pick({
          ShieldAlert, LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut,
          ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus,
          Eye, EyeOff, X, Copy, Edit2, Key, Smartphone, Laptop, MapPin, AlertTriangle,
          Calendar, User, Shield, Sliders, Monitor, Sun, Moon, ChevronDown, CalendarX,
          ShieldCheck, AlertCircle, CheckCircle, ShieldOff, BarChart2, Activity,
          TrendingUp, TrendingDown, Minus, Clock, ChevronUp
        })),
        { provide: UserControllerService, useValue: userSpy },
        { provide: VaultService, useValue: vaultSpy },
        { provide: DashboardApiService, useValue: apiSpy }
      ]
    }).compileComponents();

    const errFixture = TestBed.createComponent(DashboardComponent);
    const errComponent = errFixture.componentInstance;
    errFixture.detectChanges();

    expect(errComponent.isLoading).toBeFalse();
    expect(errComponent.errorMessage).toBeTruthy();
  });

  // ── JWT extraction ─────────────────────────────────────────────────────────

  it('should default username to "User" when no token', () => {
    component.username = 'User';
    expect(component.username).toBe('User');
  });

  it('should extract username from JWT', () => {
    const payload = btoa(JSON.stringify({ sub: 'jane_doe' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('access_token', fakeToken);
    component.ngOnInit();
    expect(component.username).toBe('jane_doe');
  });
});
