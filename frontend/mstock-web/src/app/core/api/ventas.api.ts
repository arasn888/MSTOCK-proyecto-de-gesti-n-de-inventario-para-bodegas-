import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FiltroVentas, Venta, VentaRequest, VentaResumen } from '../models/ventas';
import { parametros } from './parametros';

/** ventas-service: ventas. Registrar: ADMIN y CAJERO. Anular: solo ADMIN. */
@Injectable({ providedIn: 'root' })
export class VentasApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/ventas`;

  /** Sin fechas, el servidor devuelve las ventas de hoy. */
  listar(filtro: FiltroVentas = {}): Observable<VentaResumen[]> {
    return this.http.get<VentaResumen[]>(this.base, {
      params: parametros({ desde: filtro.desde, hasta: filtro.hasta }),
    });
  }

  obtener(id: number): Observable<Venta> {
    return this.http.get<Venta>(`${this.base}/${id}`);
  }

  /** El precio y el stock los resuelve el servidor; si falta stock responde 409 con el motivo. */
  registrar(datos: VentaRequest): Observable<Venta> {
    return this.http.post<Venta>(this.base, datos);
  }

  /** Devuelve el stock y marca la venta ANULADA. Repetirla es seguro. */
  anular(id: number, motivo: string): Observable<Venta> {
    return this.http.post<Venta>(`${this.base}/${id}/anulacion`, { motivo });
  }

  /** Boleta en PDF. */
  boleta(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/boleta`, { responseType: 'blob' });
  }
}
