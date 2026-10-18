import { ChangeDetectionStrategy, Component, Injectable, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

export interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  /** Texto del botón de confirmar (por defecto "Aceptar"). */
  aceptar?: string;
}

@Component({
  selector: 'app-confirmar-dialogo',
  imports: [MatDialogModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>{{ datos.mensaje }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true" cdkFocusInitial>
        {{ datos.aceptar ?? 'Aceptar' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmarDialogo {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
}

/** Pregunta "¿seguro?" antes de una acción destructiva. Emite true solo si el usuario acepta. */
@Injectable({ providedIn: 'root' })
export class Confirmador {
  private readonly dialogo = inject(MatDialog);

  preguntar(datos: DatosConfirmacion): Observable<boolean> {
    return this.dialogo
      .open(ConfirmarDialogo, { data: datos, width: '420px' })
      .afterClosed()
      .pipe(map((respuesta) => respuesta === true));
  }
}
