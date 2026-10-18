import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { mensajeDeError } from './errores';

function http(status: number, cuerpo?: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: cuerpo });
}

describe('mensajeDeError', () => {
  it('prefiere el mensaje que manda el servidor', () => {
    const error = http(409, { estado: 409, mensaje: 'Stock insuficiente: disponible 1, solicitado 5', fecha: 'x' });
    expect(mensajeDeError(error)).toBe('Stock insuficiente: disponible 1, solicitado 5');
  });

  it('ignora un mensaje en blanco y usa el texto según el estado', () => {
    expect(mensajeDeError(http(403, { mensaje: '   ' }))).toBe(
      'No tienes permiso para realizar esta operación',
    );
  });

  it('explica cuando no hay conexión con el gateway (estado 0)', () => {
    expect(mensajeDeError(http(0))).toContain('No se pudo conectar con el servidor');
  });

  it('traduce los estados más comunes', () => {
    expect(mensajeDeError(http(401))).toContain('sesión');
    expect(mensajeDeError(http(404))).toBe('No se encontró lo que buscas');
    expect(mensajeDeError(http(503))).toContain('no está disponible');
    expect(mensajeDeError(http(500))).toBe('Error del servidor. Inténtalo más tarde');
  });

  it('usa el texto por defecto para otros 4xx y para errores que no son HTTP', () => {
    expect(mensajeDeError(http(400), 'Revisa los datos')).toBe('Revisa los datos');
    expect(mensajeDeError(new Error('boom'))).toBe('Ocurrió un error inesperado');
    expect(mensajeDeError('texto', 'Mi mensaje')).toBe('Mi mensaje');
  });
});
