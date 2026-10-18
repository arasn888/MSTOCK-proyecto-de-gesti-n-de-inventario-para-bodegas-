import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Proveedor, ProveedorRequest } from '../models/catalogo';

/** catalogo-service: proveedores. Leer: todos los roles. Crear, editar y borrar: solo ADMIN. */
@Injectable({ providedIn: 'root' })
export class ProveedoresApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/proveedores`;

  listar(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(this.base);
  }

  obtener(id: number): Observable<Proveedor> {
    return this.http.get<Proveedor>(`${this.base}/${id}`);
  }

  crear(datos: ProveedorRequest): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.base, datos);
  }

  actualizar(id: number, datos: ProveedorRequest): Observable<Proveedor> {
    return this.http.put<Proveedor>(`${this.base}/${id}`, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
