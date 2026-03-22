import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { inject } from '@angular/core';
import { TokenRefreshService } from '../services/token-refresh.service';

/**
 * Auth Interceptor — attaches the Bearer token to every outgoing request.
 *
 * Proactive refresh: if the access token will expire within 2 minutes,
 * the interceptor silently refreshes it BEFORE sending the request,
 * so the user never experiences a 401 mid-session.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const tokenRefreshService = inject(TokenRefreshService);

  // Skip auth endpoints to avoid circular refresh loops
  const isAuthEndpoint =
    req.url.includes('/api/auth/login') ||
    req.url.includes('/api/auth/register') ||
    req.url.includes('/api/auth/refresh-token') ||
    req.url.includes('/api/auth/forgot-password') ||
    req.url.includes('/api/auth/verify-email') ||
    req.url.includes('/api/auth/verify-otp') ||
    req.url.includes('/api/auth/send-otp') ||
    req.url.includes('/api/auth/resend-otp') ||
    req.url.includes('/api/auth/resend-verification-otp') ||
    req.url.includes('/api/auth/security-questions') ||
    req.url.includes('/api/auth/reset-password') ||
    req.url.includes('/api/auth/verify-security-questions') ||
    req.url.includes('/api/auth/password-hint') ||
    req.url.includes('/api/health') ||
    req.url.includes('/api/generator');

  if (isAuthEndpoint) {
    // For public endpoints, still attach token if present (some may be optional-auth)
    const token = localStorage.getItem('access_token');
    if (token) {
      return next(addToken(req, token));
    }
    return next(req);
  }

  // For protected endpoints: check if token is about to expire
  if (tokenRefreshService.isTokenExpiredOrExpiring(120)) {
    // Token is expired or expiring within 2 minutes — proactively refresh
    return tokenRefreshService.proactiveRefreshIfNeeded(120).pipe(
      switchMap((newToken) => {
        const tokenToUse = newToken || localStorage.getItem('access_token');
        if (tokenToUse) {
          return next(addToken(req, tokenToUse));
        }
        return next(req);
      })
    );
  }

  // Token is fresh — attach it directly
  const token = localStorage.getItem('access_token');
  if (token) {
    return next(addToken(req, token));
  }

  return next(req);
};

function addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
  return request.clone({
    headers: request.headers.set('Authorization', `Bearer ${token}`)
  });
}
