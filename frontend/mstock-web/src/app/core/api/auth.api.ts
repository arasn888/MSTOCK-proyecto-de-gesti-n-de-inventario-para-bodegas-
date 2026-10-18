import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse } from '../models/auth';

/** auth-service: login. (La ruta pública del gateway es POST /api/auth/login.) */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/auth`;

  login(credenciales: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/login`, credenciales);
  }

  /** Usuario y roles según el token (sirve para comprobar que la sesión sigue siendo válida). */
  yo(): Observable<{ username: string; roles: string[] }> {
    return this.http.get<{ username: string; roles: string[] }>(`${this.base}/me`);
  }
}
