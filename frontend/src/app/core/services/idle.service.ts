import { Injectable, NgZone, inject } from '@angular/core';
import { Router } from '@angular/router';
import { fromEvent, merge, Subscription } from 'rxjs';
import { throttleTime } from 'rxjs/operators';
import { AuthenticationService } from '../api';

@Injectable({
  providedIn: 'root'
})
export class IdleService {
  private router = inject(Router);
  private authService = inject(AuthenticationService);
  private ngZone = inject(NgZone);

  /**
   * Idle timeout: 30 minutes.
   *
   * Previously this was 15 minutes — the same as the JWT access token expiry —
   * which caused forced logout every 15 minutes because the idle timer fired
   * at the exact moment the token expired, deleting the refresh token before
   * the error interceptor could use it.
   *
   * Now set to 30 minutes so the token refresh cycle (handled by the
   * auth interceptor and error interceptor) has time to silently refresh
   * the access token without the idle timer interfering.
   *
   * This value can be overridden at runtime via setTimeoutSeconds() to
   * respect the user's UserSettings.autoLockTimeout preference.
   */
  private timeoutSeconds = 30 * 60; // 30 minutes
  private timeoutId: any;
  private activitySubscription: Subscription | null = null;
  private isWatching = false;

  /**
   * Override the idle timeout at runtime (e.g. from UserSettings).
   * @param seconds Number of seconds before idle logout fires.
   */
  setTimeoutSeconds(seconds: number): void {
    this.timeoutSeconds = seconds;
    if (this.isWatching) {
      // Restart the timer with the new value
      this.resetTimer();
    }
  }

  startWatching() {
    if (this.isWatching) return;

    this.isWatching = true;

    this.ngZone.runOutsideAngular(() => {
      const events$ = merge(
        fromEvent(document, 'mousemove'),
        fromEvent(document, 'click'),
        fromEvent(document, 'keydown'),
        fromEvent(document, 'scroll')
      ).pipe(
        throttleTime(1000) // only register activity once per second at most
      );

      this.activitySubscription = events$.subscribe(() => {
        this.resetTimer();
      });
    });

    this.resetTimer();
  }

  stopWatching() {
    this.isWatching = false;
    if (this.activitySubscription) {
      this.activitySubscription.unsubscribe();
      this.activitySubscription = null;
    }
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }

  resetTimer() {
    if (!this.isWatching) return;

    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }

    this.ngZone.runOutsideAngular(() => {
      this.timeoutId = setTimeout(() => {
        this.ngZone.run(() => this.logoutDueToInactivity());
      }, this.timeoutSeconds * 1000);
    });
  }

  private logoutDueToInactivity() {
    this.stopWatching();

    const token = localStorage.getItem('access_token');

    /**
     * IMPORTANT FIX: Only remove the access token here, NOT the refresh token.
     *
     * Previously both tokens were removed, which prevented the error interceptor
     * from silently refreshing the session when the user returned. Now:
     *
     * - access_token is removed (the session is considered idle-expired)
     * - refresh_token is kept so the interceptor can silently re-authenticate
     *   on the next API call if the user returns within the refresh token window
     *
     * The backend session is still invalidated via the logout call below,
     * but the refresh token allows a new session to be created transparently.
     */
    localStorage.removeItem('access_token');
    // Do NOT remove refresh_token — let the interceptor use it to silently re-auth

    // Notify backend to invalidate the current session
    if (token) {
      this.authService.logout(`Bearer ${token}`).subscribe({
        next: () => this.navigateLogin(),
        error: () => this.navigateLogin()
      });
    } else {
      this.navigateLogin();
    }
  }

  private navigateLogin() {
    this.router.navigate(['/login']);
    // NOTE: Removed location.reload() — it was destroying all in-memory Angular
    // state unnecessarily. Navigation to /login is sufficient to clear the UI.
    // If you need to clear sensitive in-memory state, do it in component ngOnDestroy.
  }
}
