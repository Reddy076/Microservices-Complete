import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { SessionControllerService } from '../../../../core/api/api/sessionController.service';
import { SessionResponse } from '../../../../core/api/model/sessionResponse';
import { NotificationEventService } from '../../../../core/services/notification-event.service';
import { NetworkInfoService } from '../../../../core/services/network-info.service';

@Component({
  selector: 'app-sessions-settings',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './sessions-settings.component.html',
  styleUrl: './sessions-settings.component.css'
})
export class SessionsSettingsComponent implements OnInit {
  private sessionService = inject(SessionControllerService);
  private notificationEventService = inject(NotificationEventService);
  private networkInfoService = inject(NetworkInfoService);

  activeSessions: SessionResponse[] = [];
  currentSessionId: number | null = null;
  isLoadingSessions = true;
  successMessage = '';
  errorMessage = '';

  /** Precisely-detected label for the current device (resolved via userAgentData if available) */
  currentDeviceLabel = '';

  ngOnInit() {
    this.resolveCurrentDeviceLabel();
    this.loadSessions();
  }

  // ─── Current device detection ──────────────────────────────────────────────

  /**
   * Uses the modern high-entropy UA Client Hints API (Chromium ≥ 90) to get
   * the exact platform name + version so we can distinguish Windows 10 vs 11.
   * Falls back to plain UA string parsing for Firefox / Safari.
   */
  private resolveCurrentDeviceLabel() {
    const uad = (navigator as any).userAgentData;
    if (uad && typeof uad.getHighEntropyValues === 'function') {
      uad
        .getHighEntropyValues(['platform', 'platformVersion', 'brands'])
        .then((hints: any) => {
          const browser = this.detectBrowserFromBrands(
            hints.brands ?? uad.brands ?? []
          );
          const os = this.detectOsFromHints(hints);
          this.currentDeviceLabel = os ? `${browser} on ${os}` : browser;
        })
        .catch(() => {
          this.currentDeviceLabel = this.formatDeviceInfo(navigator.userAgent);
        });
    } else {
      this.currentDeviceLabel = this.formatDeviceInfo(navigator.userAgent);
    }
  }

  /** Picks the most specific brand from the brands array returned by userAgentData */
  private detectBrowserFromBrands(
    brands: Array<{ brand: string; version?: string }>
  ): string {
    const lower = (s: string) => s.toLowerCase();
    for (const b of brands) {
      if (lower(b.brand).includes('opera')) return 'Opera';
      if (lower(b.brand).includes('brave')) return 'Brave';
      if (lower(b.brand).includes('vivaldi')) return 'Vivaldi';
      if (lower(b.brand).includes('edge')) return 'Edge';
      if (lower(b.brand).includes('samsung')) return 'Samsung Internet';
      if (lower(b.brand).includes('yandex')) return 'Yandex Browser';
    }
    for (const b of brands) {
      if (lower(b.brand).includes('chrome')) return 'Chrome';
      if (lower(b.brand).includes('chromium')) return 'Chromium';
      if (lower(b.brand).includes('firefox')) return 'Firefox';
    }
    return 'Browser';
  }

  /** Converts high-entropy platform hints into a human-readable OS string */
  private detectOsFromHints(hints: any): string {
    const platform: string = hints.platform ?? '';
    const version: string = hints.platformVersion ?? '';

    if (platform === 'Windows') {
      // Windows 11 = UA Client Hints platformVersion major >= 13 (maps to NT 10.0.22000+)
      const major = parseInt(version.split('.')[0] ?? '0', 10);
      if (major >= 13) return 'Windows 11';
      if (major > 0) return 'Windows 10';
      return 'Windows';
    }
    if (platform === 'macOS' || platform === 'Mac OS X') {
      const v = version ? version.split('.').slice(0, 2).join('.') : '';
      return v ? `macOS ${v}` : 'macOS';
    }
    if (platform === 'Linux') return 'Linux';
    if (platform === 'Android') return 'Android';
    if (platform === 'iOS') return 'iOS';
    return platform || '';
  }

  // ─── Session management ────────────────────────────────────────────────────

  loadSessions() {
    this.isLoadingSessions = true;

    this.sessionService.getActiveSessions().subscribe({
      next: (sessions) => {
        this.activeSessions = sessions.sort((a, b) => {
          return (
            new Date(b.lastAccessedAt || 0).getTime() -
            new Date(a.lastAccessedAt || 0).getTime()
          );
        });
        this.isLoadingSessions = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load active sessions.';
        this.isLoadingSessions = false;
      }
    });

    const token = localStorage.getItem('access_token');
    if (token) {
      this.sessionService.getCurrentSession(`Bearer ${token}`).subscribe({
        next: (session) => {
          this.currentSessionId = session.id || null;
        }
      });
    }
  }

  /** Returns display IP — "Local Network" for loopback, real IP otherwise */
  getDisplayIp(session: SessionResponse): string {
    return this.networkInfoService.getDisplayIp(session.ipAddress);
  }

  /**
   * Returns the friendly device label for a session.
   * Uses the precisely-detected label for the CURRENT session;
   * falls back to UA string parsing for other sessions.
   */
  getDeviceLabel(session: SessionResponse): string {
    if (session.id === this.currentSessionId && this.currentDeviceLabel) {
      return this.currentDeviceLabel;
    }
    return this.formatDeviceInfo(session.deviceInfo);
  }

  onRevokeSession(sessionId: number) {
    if (!confirm('Are you sure you want to log out this device?')) return;
    this.errorMessage = '';
    this.successMessage = '';

    this.sessionService.terminateSession(sessionId).subscribe({
      next: () => {
        this.successMessage = 'Device logged out successfully.';
        this.loadSessions();
        this.notificationEventService.triggerRefresh();
      },
      error: () => {
        this.errorMessage = 'Failed to log out device. Please try again.';
      }
    });
  }

  onRevokeAllOtherSessions() {
    if (!confirm('Are you sure you want to log out of all other devices?')) return;
    this.errorMessage = '';
    this.successMessage = '';

    this.sessionService.terminateAllSessions().subscribe({
      next: () => {
        this.successMessage = 'All other devices logged out successfully.';
        this.loadSessions();
        this.notificationEventService.triggerRefresh();
      },
      error: () => {
        this.errorMessage = 'Failed to log out other devices. Please try again.';
      }
    });
  }

  // ─── UA-string fallback parser ─────────────────────────────────────────────

  /**
   * Parses a raw User-Agent string into a friendly "Browser on OS" label.
   * Used for sessions from OTHER devices where we only have the stored UA.
   *
   * NOTE: Windows 10 and 11 both report "Windows NT 10.0" in the UA string —
   * Microsoft intentionally kept this for compatibility. We cannot distinguish
   * them here; use getDeviceLabel() for the current session to get the exact version.
   */
  formatDeviceInfo(deviceInfo: string | undefined): string {
    if (!deviceInfo) return 'Unknown Device';

    let browser = 'Browser';
    let os = '';

    // Browser detection — order matters.
    // Edge and Opera both include "Chrome" in their UA, so check them first.
    if (/Brave/i.test(deviceInfo)) {
      browser = 'Brave';
    } else if (/OPR\/|OPiOS\/|Opera\//i.test(deviceInfo)) {
      browser = 'Opera';
    } else if (/Vivaldi/i.test(deviceInfo)) {
      browser = 'Vivaldi';
    } else if (/Edg\/|EdgA\/|EdgiOS\//i.test(deviceInfo)) {
      // Must be before Chrome check — Edge UA includes "Chrome"
      browser = 'Edge';
    } else if (/SamsungBrowser/i.test(deviceInfo)) {
      browser = 'Samsung Internet';
    } else if (/UCBrowser/i.test(deviceInfo)) {
      browser = 'UC Browser';
    } else if (/YaBrowser/i.test(deviceInfo)) {
      browser = 'Yandex Browser';
    } else if (/Firefox|FxiOS/i.test(deviceInfo)) {
      browser = 'Firefox';
    } else if (/CriOS|Chrome|Chromium/i.test(deviceInfo)) {
      browser = 'Chrome';
    } else if (/Safari/i.test(deviceInfo) && !/Chrome|Chromium/i.test(deviceInfo)) {
      browser = 'Safari';
    }

    // OS detection
    const androidMatch = deviceInfo.match(/Android (\d+(?:\.\d+)?)/);
    const macMatch = deviceInfo.match(/Mac OS X ([\d_]+)/);

    if (/Windows NT 10\.0/.test(deviceInfo)) {
      // Both Win10 and Win11 report NT 10.0 — cannot distinguish from UA alone
      os = 'Windows 10/11';
    } else if (/Windows NT 6\.3/.test(deviceInfo)) {
      os = 'Windows 8.1';
    } else if (/Windows NT 6\.2/.test(deviceInfo)) {
      os = 'Windows 8';
    } else if (/Windows/.test(deviceInfo)) {
      os = 'Windows';
    } else if (androidMatch) {
      os = `Android ${androidMatch[1]}`;
    } else if (/iPhone/.test(deviceInfo)) {
      os = 'iPhone';
    } else if (/iPad/.test(deviceInfo)) {
      os = 'iPad';
    } else if (macMatch) {
      const v = macMatch[1].replace(/_/g, '.').split('.').slice(0, 2).join('.');
      os = `macOS ${v}`;
    } else if (/Linux/.test(deviceInfo)) {
      os = 'Linux';
    }

    const label = os ? `${browser} on ${os}` : browser;
    return label.length > 60 ? label.substring(0, 60) + '...' : label;
  }
}
