import { HttpParams } from '@angular/common/http';

type Valor = string | number | boolean | null | undefined;

/** Arma los query params sin enviar los que están vacíos (undefined, null o texto en blanco). */
export function parametros(valores: Record<string, Valor>): HttpParams {
  let params = new HttpParams();
  for (const [clave, valor] of Object.entries(valores)) {
    if (valor === undefined || valor === null) {
      continue;
    }
    if (typeof valor === 'string' && valor.trim() === '') {
      continue;
    }
    params = params.set(clave, String(valor).trim());
  }
  return params;
}
