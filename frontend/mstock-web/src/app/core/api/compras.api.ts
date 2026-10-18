import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Compra, CompraRequest } from '../models/compras';

/** compras-service (ADMIN y ALMACENERO). El ingreso al inventario es asíncrono: la compra nace PENDIENTE. */
@Injectable({ providedIn: 'root' })
export class ComprasApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/compras`;

  listar(): Observable<Compra[]> {
    return this.http.get<Compra[]>(this.base);
  }

  obtener(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${this.base}/${id}`);
  }

  registrar(datos: CompraRequest): Observable<Compra> {
    return this.http.post<Compra>(this.base, datos);
  }
}
