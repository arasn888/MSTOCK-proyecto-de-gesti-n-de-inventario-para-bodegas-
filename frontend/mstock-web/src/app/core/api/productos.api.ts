import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FiltroProductos, Producto, ProductoRequest } from '../models/catalogo';
import { parametros } from './parametros';

/** catalogo-service: productos. Leer: todos los roles. Crear, editar y borrar: solo ADMIN. */
@Injectable({ providedIn: 'root' })
export class ProductosApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/productos`;

  /** Búsqueda por nombre (o código) y/o categoría. Sin filtros trae todos. */
  listar(filtro: FiltroProductos = {}): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.base, {
      params: parametros({ nombre: filtro.nombre, categoriaId: filtro.categoriaId }),
    });
  }

  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.base}/${id}`);
  }

  crear(datos: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.base, datos);
  }

  actualizar(id: number, datos: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.base}/${id}`, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
