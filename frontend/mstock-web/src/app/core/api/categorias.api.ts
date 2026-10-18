import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Categoria, CategoriaRequest } from '../models/catalogo';

/** catalogo-service: categorías. Leer: todos los roles. Crear, editar y borrar: solo ADMIN. */
@Injectable({ providedIn: 'root' })
export class CategoriasApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/categorias`;

  listar(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.base);
  }

  obtener(id: number): Observable<Categoria> {
    return this.http.get<Categoria>(`${this.base}/${id}`);
  }

  crear(datos: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.base, datos);
  }

  actualizar(id: number, datos: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.base}/${id}`, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
