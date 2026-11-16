import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Permite acceso solo a usuarios autenticados con rol Jefe. */
export const chiefGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isChief()) {
    return true;
  }
  return inject(Router).createUrlTree(['/']);
};
