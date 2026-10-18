import { HttpErrorResponse } from '@angular/common/http';

/** Formato de error de TODOS los servicios y del gateway: { estado, mensaje, fecha }. */
export interface ApiError {
  estado: number;
  mensaje: string;
  fecha: string;
}

function esApiError(cuerpo: unknown): cuerpo is ApiError {
  return (
    typeof cuerpo === 'object' &&
    cuerpo !== null &&
    typeof (cuerpo as { mensaje?: unknown }).mensaje === 'string'
  );
}

/**
 * Convierte cualquier error en un texto para mostrar al usuario. Prefiere el mensaje que manda el
 * servidor (ya viene en español y con la regla de negocio, p. ej. "Stock insuficiente...").
 */
export function mensajeDeError(error: unknown, porDefecto = 'Ocurrió un error inesperado'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return porDefecto;
  }
  if (esApiError(error.error) && error.error.mensaje.trim() !== '') {
    return error.error.mensaje;
  }
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el gateway esté encendido';
    case 401:
      return 'Tu sesión no es válida o expiró. Inicia sesión de nuevo';
    case 403:
      return 'No tienes permiso para realizar esta operación';
    case 404:
      return 'No se encontró lo que buscas';
    case 503:
      return 'El servicio no está disponible en este momento. Inténtalo en unos segundos';
    default:
      return error.status >= 500 ? 'Error del servidor. Inténtalo más tarde' : porDefecto;
  }
}
