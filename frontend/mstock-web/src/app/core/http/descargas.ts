import { HttpResponse } from '@angular/common/http';

/**
 * Nombre del archivo según la cabecera Content-Disposition. El navegador solo puede leerla porque el
 * gateway la expone en CORS (Access-Control-Expose-Headers).
 */
export function nombreDeArchivo(respuesta: HttpResponse<Blob>, porDefecto: string): string {
  const cabecera = respuesta.headers.get('Content-Disposition');
  if (!cabecera) {
    return porDefecto;
  }
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(cabecera);
  if (utf8) {
    try {
      return decodeURIComponent(utf8[1]);
    } catch {
      // cae al formato simple
    }
  }
  const simple = /filename="?([^";]+)"?/i.exec(cabecera);
  return simple ? simple[1] : porDefecto;
}

/** Descarga el blob como archivo. */
export function guardarArchivo(blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Abre el PDF en otra pestaña; si el navegador bloquea la ventana emergente, lo descarga. */
export function abrirPdf(blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
  const ventana = window.open(url, '_blank');
  if (!ventana) {
    guardarArchivo(blob, nombre);
  }
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
