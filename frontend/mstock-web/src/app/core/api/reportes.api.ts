import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CierreCaja, Dashboard } from '../models/ventas';
import { parametros } from './parametros';

/** ventas-service: reportes. Dashboard: solo ADMIN. Cierre de caja: ADMIN (todos) y CAJERO (solo el suyo). */
@Injectable({ providedIn: 'root' })
export class ReportesApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/reportes`;

  /** Fechas ISO (yyyy-MM-dd), ambas incluidas. Sin fechas: hoy. `top` entre 1 y 50. */
  dashboard(desde?: string, hasta?: string, top?: number): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.base}/dashboard`, {
      params: parametros({ desde, hasta, top }),
    });
  }

  cierreCaja(fecha?: string): Observable<CierreCaja> {
    return this.http.get<CierreCaja>(`${this.base}/cierre-caja`, { params: parametros({ fecha }) });
  }
}
