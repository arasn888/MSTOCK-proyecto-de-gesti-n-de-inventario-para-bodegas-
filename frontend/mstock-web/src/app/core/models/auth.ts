export type Rol = 'ADMIN' | 'CAJERO' | 'ALMACENERO';

export const ROLES: readonly Rol[] = ['ADMIN', 'CAJERO', 'ALMACENERO'];

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEnSegundos: number;
  username: string;
  rol: Rol;
}

/** Lo que se guarda en el navegador tras el login. `expiraEn` es una marca de tiempo en milisegundos. */
export interface Sesion {
  token: string;
  username: string;
  rol: Rol;
  expiraEn: number;
}

export interface Usuario {
  id: number;
  username: string;
  nombres: string;
  apellidos: string;
  email: string;
  rol: Rol;
  activo: boolean;
  fechaCreacion: string;
}

export interface UsuarioRequest {
  username: string;
  password: string;
  nombres: string;
  apellidos: string;
  email: string;
  rol: Rol;
}

export interface UsuarioUpdateRequest {
  nombres: string;
  apellidos: string;
  email: string;
  rol: Rol;
  activo: boolean;
  /** Opcional: si no se envía, la contraseña no cambia. */
  password?: string;
}
