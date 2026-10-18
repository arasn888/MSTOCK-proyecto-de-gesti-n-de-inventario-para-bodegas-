import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { finalize } from 'rxjs';
import { InventarioApi } from '../../core/api/inventario.api';
import { guardarArchivo, nombreDeArchivo } from '../../core/http/descargas';
import { Movimiento } from '../../core/models/inventario';
import { Notificador } from '../../core/notificador.service';

export interface DatosKardex {
  productoId: number;
  nombre: string;
}

/** Historial de movimientos de un producto (el más reciente primero) y descarga en CSV. */
@Component({
  selector: 'app-kardex-dialogo',
  imports: [DatePipe, MatDialogModule, MatTableModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Kardex · {{ datos.nombre }}</h2>
    <mat-dialog-content>
      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      <div class="tabla">
        <table mat-table [dataSource]="movimientos()">
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let m">{{ m.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let m">
              <span class="tipo" [class]="'tipo ' + m.tipo.toLowerCase()">{{ m.tipo }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="cantidad">
            <th mat-header-cell *matHeaderCellDef class="num">Cantidad</th>
            <td mat-cell *matCellDef="let m" class="num">{{ m.cantidad }}</td>
          </ng-container>
          <ng-container matColumnDef="stock">
            <th mat-header-cell *matHeaderCellDef class="num">Stock</th>
            <td mat-cell *matCellDef="let m" class="num">{{ m.stockAnterior }} → {{ m.stockResultante }}</td>
          </ng-container>
          <ng-container matColumnDef="motivo">
            <th mat-header-cell *matHeaderCellDef>Motivo</th>
            <td mat-cell *matCellDef="let m">{{ m.motivo ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="usuario">
            <th mat-header-cell *matHeaderCellDef>Usuario</th>
            <td mat-cell *matCellDef="let m">{{ m.usuario }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">
              {{ cargando() ? 'Cargando…' : 'Este producto todavía no tiene movimientos' }}
            </td>
          </tr>
        </table>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [disabled]="descargando() || movimientos().length === 0"
              (click)="descargarCsv()">
        <mat-icon>download</mat-icon> Descargar CSV
      </button>
      <button mat-flat-button type="button" mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .tabla { overflow-x: auto; min-width: min(760px, 85vw); }
    table { width: 100%; }
    .num { text-align: right; white-space: nowrap; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
    .tipo { font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #eee; }
    .tipo.entrada { background: #e6f4ea; color: #1e6b34; }
    .tipo.salida { background: #fdecea; color: #a1281c; }
    .tipo.ajuste { background: #e8f0fe; color: #1a4fa0; }
  `,
})
export class KardexDialogo {
  protected readonly datos = inject<DatosKardex>(MAT_DIALOG_DATA);
  private readonly api = inject(InventarioApi);
  private readonly notificador = inject(Notificador);

  protected readonly columnas = ['fecha', 'tipo', 'cantidad', 'stock', 'motivo', 'usuario'];
  protected readonly movimientos = signal<Movimiento[]>([]);
  protected readonly cargando = signal(true);
  protected readonly descargando = signal(false);

  constructor() {
    this.api
      .kardex(this.datos.productoId)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (movimientos) => this.movimientos.set(movimientos),
        error: (error: unknown) => this.notificador.error(error, 'No se pudo cargar el kardex'),
      });
  }

  protected descargarCsv(): void {
    this.descargando.set(true);
    this.api
      .descargarKardexCsv(this.datos.productoId)
      .pipe(finalize(() => this.descargando.set(false)))
      .subscribe({
        next: (respuesta) => {
          if (respuesta.body) {
            guardarArchivo(respuesta.body, nombreDeArchivo(respuesta, 'kardex.csv'));
          }
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo descargar el kardex'),
      });
  }
}
