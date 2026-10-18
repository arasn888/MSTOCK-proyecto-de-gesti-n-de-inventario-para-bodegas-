import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/auth';
import { Notificador } from '../notificador.service';
import { SesionService } from './sesion.service';

/** Solo con sesión vigente; si no, al login recordando a dónde quería ir. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  if (sesion.estaAutenticado()) {
    return true;
  }
  sesion.cerrar();
  return router.createUrlTree(['/login'], { queryParams: { volverA: estado.url } });
};

/** La pantalla de login solo para quien no tiene sesión: si ya la tiene, al inicio. */
export const invitadoGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  return sesion.estaAutenticado() ? inject(Router).createUrlTree(['/inicio']) : true;
};

/** Solo para ciertos roles; el resto vuelve al inicio con un aviso. */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => {
    const sesion = inject(SesionService);
    const router = inject(Router);
    if (sesion.tieneRol(roles)) {
      return true;
    }
    inject(Notificador).aviso('No tienes permiso para abrir esa pantalla');
    return router.createUrlTree(['/inicio']);
  };
}
