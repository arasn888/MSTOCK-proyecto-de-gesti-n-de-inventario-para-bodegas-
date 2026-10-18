import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { nombreDeArchivo } from './descargas';

function respuesta(contentDisposition?: string): HttpResponse<Blob> {
  const headers = contentDisposition
    ? new HttpHeaders({ 'Content-Disposition': contentDisposition })
    : new HttpHeaders();
  return new HttpResponse<Blob>({ body: new Blob(['x']), headers });
}

describe('nombreDeArchivo', () => {
  it('usa el nombre por defecto si no hay cabecera', () => {
    expect(nombreDeArchivo(respuesta(), 'kardex.csv')).toBe('kardex.csv');
  });

  it('lee el formato simple', () => {
    expect(nombreDeArchivo(respuesta('attachment; filename="boleta-B001-00000007.pdf"'), 'x.pdf')).toBe(
      'boleta-B001-00000007.pdf',
    );
  });

  it('prefiere el formato UTF-8 y lo decodifica', () => {
    const cabecera = "attachment; filename=\"kardex.csv\"; filename*=UTF-8''kardex%20az%C3%BAcar.csv";
    expect(nombreDeArchivo(respuesta(cabecera), 'x.csv')).toBe('kardex azúcar.csv');
  });
});
