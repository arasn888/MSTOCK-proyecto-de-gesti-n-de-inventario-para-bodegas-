import { Injectable, inject, signal } from '@angular/core';
import { NotificacionesApi } from './api/notificaciones.api';

/**
 * Cantidad de notificaciones sin leer (el globo de la campana). Lo consulta el menú cada 30 segundos y la
 * pantalla de notificaciones lo refresca al instante cuando se marca o borra algo.
 */
@Injectable({ providedIn: 'root' })
export class ContadorNotificaciones {
  private readonly api = inject(NotificacionesApi);
  private readonly cantidad = signal(0);

  readonly noLeidas = this.cantidad.asReadonly();

  refrescar(): void {
    this.api.resumen().subscribe({
      next: (resumen) => this.cantidad.set(resumen.noLeidas),
      error: () => this.cantidad.set(0),
    });
  }
}
