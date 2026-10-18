import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Cliente, ClienteRequest } from '../models/ventas';
import { parametros } from './parametros';

/** ventas-service: clientes. Crear y editar: ADMIN y CAJERO. Borrar: solo ADMIN. */
@Injectable({ providedIn: 'root' })
export class ClientesApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/clientes`;

  listar(nombre?: string): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.base, { params: parametros({ nombre }) });
  }

  obtener(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.base}/${id}`);
  }

  crear(datos: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(this.base, datos);
  }

  actualizar(id: number, datos: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.base}/${id}`, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
