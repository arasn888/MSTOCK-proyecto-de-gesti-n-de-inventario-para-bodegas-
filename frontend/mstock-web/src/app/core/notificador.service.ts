import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { mensajeDeError } from './http/errores';

/** Mensajes breves al usuario (éxito, aviso o error de la API) con un solo estilo en toda la app. */
@Injectable({ providedIn: 'root' })
export class Notificador {
  private readonly snack = inject(MatSnackBar);

  ok(mensaje: string): void {
    this.snack.open(mensaje, 'Cerrar', { duration: 3500 });
  }

  aviso(mensaje: string): void {
    this.snack.open(mensaje, 'Cerrar', { duration: 5000 });
  }

  error(error: unknown, porDefecto?: string): void {
    this.snack.open(mensajeDeError(error, porDefecto), 'Cerrar', { duration: 7000 });
  }
}
