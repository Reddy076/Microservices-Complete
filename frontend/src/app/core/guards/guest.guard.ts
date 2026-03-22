import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const guestGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('access_token');

  if (token) {
    // If the user already has a token, they shouldn't be on the landing page or login page
    return router.createUrlTree(['/dashboard']);
  }

  // Allow access to public routes
  return true;
};
