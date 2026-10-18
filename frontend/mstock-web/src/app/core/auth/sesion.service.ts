import { Injectable, computed, signal } from '@angular/core';
import { LoginResponse, Rol, Sesion } from '../models/auth';

const CLAVE = 'mstock.sesion';

/**
 * Sesión del usuario: token JWT, usuario y rol. Se guarda en localStorage para sobrevivir a recargar la
 * página. La seguridad real está en los servicios (validan el JWT y el rol en cada petición); esto solo
 * decide qué se muestra y a dónde se puede navegar.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly actual = signal<Sesion | null>(this.leer());

  readonly sesion = this.actual.asReadonly();
  readonly usuario = computed(() => this.actual()?.username ?? null);
  readonly rol = computed(() => this.actual()?.rol ?? null);
  readonly token = computed(() => this.actual()?.token ?? null);

  /** Hay sesión y el token no ha expirado (se evalúa al llamarlo, no es reactivo al paso del tiempo). */
  estaAutenticado(): boolean {
    const sesion = this.actual();
    return sesion !== null && sesion.expiraEn > Date.now();
  }

  tieneRol(roles: readonly Rol[]): boolean {
    const rol = this.rol();
    return rol !== null && roles.includes(rol);
  }

  iniciar(respuesta: LoginResponse): void {
    const sesion: Sesion = {
      token: respuesta.token,
      username: respuesta.username,
      rol: respuesta.rol,
      expiraEn: Date.now() + respuesta.expiraEnSegundos * 1000,
    };
    this.actual.set(sesion);
    try {
      localStorage.setItem(CLAVE, JSON.stringify(sesion));
    } catch {
      // sin almacenamiento disponible: la sesión vive solo mientras no se recargue la página
    }
  }

  cerrar(): void {
    this.actual.set(null);
    try {
      localStorage.removeItem(CLAVE);
    } catch {
      // nada que limpiar
    }
  }

  private leer(): Sesion | null {
    try {
      const texto = localStorage.getItem(CLAVE);
      if (!texto) {
        return null;
      }
      const sesion = JSON.parse(texto) as Partial<Sesion>;
      const valida =
        typeof sesion.token === 'string' &&
        typeof sesion.username === 'string' &&
        typeof sesion.rol === 'string' &&
        typeof sesion.expiraEn === 'number' &&
        sesion.expiraEn > Date.now();
      return valida ? (sesion as Sesion) : null;
    } catch {
      return null;
    }
  }
}
