import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LayoutComponent } from './layout.component';
import { Router, provideRouter } from '@angular/router';
import { AuthenticationService } from '../core/api';
import { of, throwError } from 'rxjs';
import { IdleService } from '../core/services/idle.service';
import { NotificationControllerService } from '../core/api/api/notificationController.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { importProvidersFrom } from '@angular/core';
import {
  LucideAngularModule, LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut,
  ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus, Eye,
  EyeOff, X, Copy, Edit2, ShieldAlert, Key, Smartphone, Laptop, MapPin, AlertTriangle,
  Calendar, User, Shield, Sliders, Monitor, Sun, Moon, ChevronDown, CalendarX, ShieldCheck,
  AlertCircle, CheckCircle, ShieldOff, BarChart2, Activity, TrendingUp, TrendingDown, Menu, Minus, Clock, ChevronUp,
  Download, Upload, History as HistoryIcon
} from 'lucide-angular';

describe('LayoutComponent', () => {
  let component: LayoutComponent;
  let fixture: ComponentFixture<LayoutComponent>;
  let router: Router;
  let authSpy: jasmine.SpyObj<AuthenticationService>;
  let idleSpy: jasmine.SpyObj<IdleService>;

  beforeEach(async () => {
    localStorage.clear();

    authSpy = jasmine.createSpyObj('AuthenticationService', ['logout']);
    authSpy.logout.and.returnValue(of({ message: 'Logged out' }) as any);

    idleSpy = jasmine.createSpyObj('IdleService', ['startWatching', 'stopWatching']);

    const notifSpy = jasmine.createSpyObj('NotificationControllerService', [
      'getUnreadCount', 'getNotifications', 'markAsRead', 'markAllAsRead', 'deleteNotification'
    ]);
    notifSpy.getUnreadCount.and.returnValue(of({ unreadCount: 0 }) as any);
    notifSpy.getNotifications.and.returnValue(of([]) as any);

    await TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        importProvidersFrom(LucideAngularModule.pick({
          LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut, ChevronLeft,
          ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus, Eye, EyeOff,
          X, Copy, Edit2, ShieldAlert, Key, Smartphone, Laptop, MapPin, AlertTriangle,
          Calendar, User, Shield, Sliders, Monitor, Sun, Moon, ChevronDown, CalendarX,
          ShieldCheck, AlertCircle, CheckCircle, ShieldOff, BarChart2, Activity,
          TrendingUp, TrendingDown, Menu, Minus, Clock, ChevronUp, Download, Upload, History: HistoryIcon
        })),
        { provide: AuthenticationService, useValue: authSpy },
        { provide: IdleService, useValue: idleSpy },
        { provide: NotificationControllerService, useValue: notifSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LayoutComponent);
    component = fixture.componentInstance;

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create and start watching on init', () => {
    expect(component).toBeTruthy();
    expect(idleSpy.startWatching).toHaveBeenCalled();
  });

  it('should stop watching on destroy', () => {
    component.ngOnDestroy();
    expect(idleSpy.stopWatching).toHaveBeenCalled();
  });

  it('should toggle sidebar', () => {
    expect(component.sidebarCollapsed).toBeFalse();
    component.toggleSidebar();
    expect(component.sidebarCollapsed).toBeTrue();
    component.toggleSidebar();
    expect(component.sidebarCollapsed).toBeFalse();
  });

  it('should stop watching, clear tokens and navigate to login on logout when API succeeds', () => {
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('refresh_token', 'test-refresh');

    component.logout();

    expect(idleSpy.stopWatching).toHaveBeenCalled();
    expect(authSpy.logout).toHaveBeenCalledWith('Bearer test-token');
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should clear tokens and navigate to login on logout even when API fails', () => {
    authSpy.logout.and.returnValue(throwError(() => new Error('API Error')));
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('refresh_token', 'test-refresh');

    component.logout();

    expect(authSpy.logout).toHaveBeenCalledWith('Bearer test-token');
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should handle logout when no token exists', () => {
    component.logout();

    expect(authSpy.logout).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should return "User" when no token', () => {
    expect(component.currentUsername).toBe('User');
  });

  it('should extract username from JWT', () => {
    const payload = btoa(JSON.stringify({ sub: 'john_doe' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('access_token', fakeToken);
    expect(component.currentUsername).toBe('john_doe');
  });
});
