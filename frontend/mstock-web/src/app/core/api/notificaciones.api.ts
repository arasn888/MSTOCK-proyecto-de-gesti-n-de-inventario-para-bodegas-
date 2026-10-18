import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LecturaMasiva, Notificacion, ResumenNotificaciones } from '../models/notificaciones';
import { parametros } from './parametros';

/** notificaciones-service. Leer: todos los roles. Marcar leídas y borrar: ADMIN y ALMACENERO. */
@Injectable({ providedIn: 'root' })
export class NotificacionesApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/notificaciones`;

  listar(soloNoLeidas = false): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(this.base, { params: parametros({ soloNoLeidas }) });
  }

  /** Cantidad de no leídas (para el globo de la campana). */
  resumen(): Observable<ResumenNotificaciones> {
    return this.http.get<ResumenNotificaciones>(`${this.base}/resumen`);
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.put<Notificacion>(`${this.base}/${id}/leida`, {});
  }

  marcarTodasLeidas(): Observable<LecturaMasiva> {
    return this.http.put<LecturaMasiva>(`${this.base}/leer-todas`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
