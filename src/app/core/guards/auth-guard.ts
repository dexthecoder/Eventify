import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { filter, map, take, switchMap } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.initialized$.pipe(
    filter(initialized => initialized === true),
    take(1),
    switchMap(() => authService.currentUser$.pipe(
      take(1),
      map(user => {
        if (user !== null) return true;
        router.navigate(['/login']);
        return false;
      })
    ))
  );
};