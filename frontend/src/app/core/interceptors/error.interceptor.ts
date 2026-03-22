import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { inject } from '@angular/core';
import { TokenRefreshService } from '../services/token-refresh.service';

/**
 * Error Interceptor — handles HTTP errors globally.
 *
 * On 401 (Unauthorized):
 *   - Skips refresh for login/refresh-token/sensitive endpoints
 *   - Delegates to TokenRefreshService.handleTokenRefresh() which:
 *       1. Uses the stored refresh token to get a new access token
 *       2. Queues concurrent requests until the new token is ready
 *       3. Retries the original request with the new token
 *       4. Logs out and redirects to /login if refresh fails
 *
 * Module-level state bug is fixed: all state lives in the injectable
 * TokenRefreshService (singleton), not in module-level variables.
 */
export const errorInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const tokenRefreshService = inject(TokenRefreshService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        (error.status === 401 || error.status === 403) &&
        !req.url.includes('/api/auth/login') &&
        !req.url.includes('/api/auth/refresh-token') &&
        !req.url.includes('/sensitive-view') &&
        !req.url.includes('/view-password')
      ) {
        return tokenRefreshService.handleTokenRefresh(req as HttpRequest<any>, next);
      }
      return throwError(() => error);
    })
  );
};
