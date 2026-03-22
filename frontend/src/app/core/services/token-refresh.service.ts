import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthenticationService } from '../api';

@Injectable({
  providedIn: 'root'
})
export class TokenRefreshService {
  private router = inject(Router);
  private authService = inject(AuthenticationService);

  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  /**
   * Attempt to refresh the access token using the stored refresh token.
   * Queues any concurrent requests until the new token is available.
   */
  handleTokenRefresh(
    req: HttpRequest<any>,
    next: HttpHandlerFn
  ): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = localStorage.getItem('refresh_token');

      if (refreshToken) {
        return this.authService.refreshToken({ refreshToken }).pipe(
          switchMap((tokenResponse: any) => {
            this.isRefreshing = false;

            if (tokenResponse?.accessToken) {
              localStorage.setItem('access_token', tokenResponse.accessToken);
              if (tokenResponse.refreshToken) {
                localStorage.setItem('refresh_token', tokenResponse.refreshToken);
              }
              this.refreshTokenSubject.next(tokenResponse.accessToken);
              return next(this.addToken(req, tokenResponse.accessToken));
            } else {
              return this.logoutAndRedirect();
            }
          }),
          catchError((err) => {
            this.isRefreshing = false;
            this.refreshTokenSubject.next(null);
            if (err.status === 401 || err.status === 403) {
              return this.logoutAndRedirect();
            }
            return throwError(() => err);
          })
        );
      } else {
        this.isRefreshing = false;
        return this.logoutAndRedirect();
      }
    } else {
      // Another refresh is already in progress — queue this request
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => next(this.addToken(req, token as string)))
      );
    }
  }

  /**
   * Proactively refresh the access token if it expires within the given
   * threshold (default: 2 minutes = 120 seconds).
   * Returns the new access token, or null if no refresh was needed.
   */
  proactiveRefreshIfNeeded(thresholdSeconds = 120): Observable<string | null> {
    const token = localStorage.getItem('access_token');
    if (!token) {
      return new BehaviorSubject<string | null>(null).asObservable();
    }

    const expiresAt = this.getTokenExpiry(token);
    if (expiresAt === null) {
      return new BehaviorSubject<string | null>(null).asObservable();
    }

    const secondsUntilExpiry = (expiresAt - Date.now()) / 1000;

    if (secondsUntilExpiry > thresholdSeconds) {
      // Token is still fresh — no refresh needed
      return new BehaviorSubject<string | null>(null).asObservable();
    }

    // Token is about to expire — refresh it
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      return new BehaviorSubject<string | null>(null).asObservable();
    }

    return this.authService.refreshToken({ refreshToken }).pipe(
      switchMap((tokenResponse: any) => {
        if (tokenResponse?.accessToken) {
          localStorage.setItem('access_token', tokenResponse.accessToken);
          if (tokenResponse.refreshToken) {
            localStorage.setItem('refresh_token', tokenResponse.refreshToken);
          }
          const subject = new BehaviorSubject<string | null>(tokenResponse.accessToken);
          return subject.asObservable();
        }
        return new BehaviorSubject<string | null>(null).asObservable();
      }),
      catchError(() => new BehaviorSubject<string | null>(null).asObservable())
    );
  }

  /**
   * Decode the JWT expiry timestamp (milliseconds since epoch).
   * Returns null if the token is malformed.
   */
  getTokenExpiry(token: string): number | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = JSON.parse(atob(parts[1]));
      if (!payload.exp) return null;
      return payload.exp * 1000; // convert seconds → milliseconds
    } catch {
      return null;
    }
  }

  /**
   * Check whether the stored access token is expired or about to expire.
   */
  isTokenExpiredOrExpiring(thresholdSeconds = 120): boolean {
    const token = localStorage.getItem('access_token');
    if (!token) return true;
    const expiresAt = this.getTokenExpiry(token);
    if (expiresAt === null) return true;
    return (expiresAt - Date.now()) / 1000 < thresholdSeconds;
  }

  logoutAndRedirect(): Observable<never> {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    this.router.navigate(['/login']);
    return throwError(() => new Error('Session expired. Please log in again.'));
  }

  private addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      headers: request.headers.set('Authorization', `Bearer ${token}`)
    });
  }
}
