import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SesionService } from './sesion.service';

const LOGIN = '/api/auth/login';

/**
 * Adjunta el JWT a cada petición hacia el gateway (nunca a otros dominios) y, si el servidor responde 401
 * (token vencido o inválido), cierra la sesión y manda al login.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(SesionService);
  const router = inject(Router);

  const haciaLaApi = peticion.url.startsWith(environment.apiUrl);
  const esLogin = peticion.url.endsWith(LOGIN);
  const token = sesion.token();

  const saliente =
    haciaLaApi && !esLogin && token
      ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : peticion;

  return siguiente(saliente).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && haciaLaApi && !esLogin) {
        sesion.cerrar();
        void router.navigate(['/login'], { queryParams: { expirada: 1 } });
      }
      return throwError(() => error);
    }),
  );
};
