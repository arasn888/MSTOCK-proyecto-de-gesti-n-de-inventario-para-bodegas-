import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AjusteRequest, Movimiento, MovimientoRequest, Stock } from '../models/inventario';

/**
 * inventario-service. Ver stock: todos los roles. Entradas, salidas, ajustes y stock mínimo: ADMIN y
 * ALMACENERO (la salida también el CAJERO). El descuento por venta lo hace ventas-service, no el frontend.
 */
@Injectable({ providedIn: 'root' })
export class InventarioApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/inventario`;

  listar(): Observable<Stock[]> {
    return this.http.get<Stock[]>(this.base);
  }

  /** Productos con stock en el mínimo o por debajo (stock crítico). */
  alertas(): Observable<Stock[]> {
    return this.http.get<Stock[]>(`${this.base}/alertas`);
  }

  obtener(productoId: number): Observable<Stock> {
    return this.http.get<Stock>(`${this.base}/${productoId}`);
  }

  /** Kardex: del movimiento más reciente al más antiguo. */
  kardex(productoId: number): Observable<Movimiento[]> {
    return this.http.get<Movimiento[]>(`${this.base}/${productoId}/movimientos`);
  }

  /** Kardex en CSV (ADMIN y ALMACENERO). Devuelve la respuesta completa para leer el nombre del archivo. */
  descargarKardexCsv(productoId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.base}/${productoId}/kardex/csv`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  entrada(datos: MovimientoRequest): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${this.base}/entradas`, datos);
  }

  salida(datos: MovimientoRequest): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${this.base}/salidas`, datos);
  }

  /** Corrige el stock con el conteo físico real; deja la diferencia en el kardex. */
  ajustar(datos: AjusteRequest): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${this.base}/ajustes`, datos);
  }

  actualizarStockMinimo(productoId: number, stockMinimo: number): Observable<Stock> {
    return this.http.put<Stock>(`${this.base}/${productoId}/stock-minimo`, { stockMinimo });
  }
}
