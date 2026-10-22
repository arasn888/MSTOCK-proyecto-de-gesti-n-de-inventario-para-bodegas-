import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { Compra } from '../../core/models/compras';

export interface DatosCompraDetalle {
  compra: Compra;
  proveedor: string;
}

/** Detalle de una compra: líneas, estado y, si el inventario la rechazó, el motivo. */
@Component({
  selector: 'app-compra-detalle',
  imports: [DatePipe, MatDialogModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Compra #{{ datos.compra.id }}</h2>
    <mat-dialog-content>
      <p class="datos">
        {{ datos.compra.fecha | date: 'dd/MM/yyyy HH:mm' }} · {{ datos.proveedor }} · Registró: {{ datos.compra.usuario }}
      </p>
      @switch (datos.compra.estado) {
        @case ('PENDIENTE') {
          <p class="estado pendiente" role="status">
            Pendiente: el inventario aún no confirma el ingreso. Se actualiza en unos segundos.
          </p>
        }
        @case ('RECIBIDA') {
          <p class="estado recibida" role="status">Recibida: el stock ya se sumó al inventario.</p>
        }
        @case ('RECHAZADA') {
          <p class="estado rechazada" role="status">
            Rechazada por el inventario: {{ datos.compra.motivoRechazo ?? 'sin motivo informado' }}
          </p>
        }
      }
      <table class="detalle">
        <thead>
          <tr><th>Producto</th><th class="num">Cant.</th><th class="num">Costo</th><th class="num">Subtotal</th></tr>
        </thead>
        <tbody>
          @for (d of datos.compra.items; track d.id) {
            <tr>
              <td>{{ d.productoNombre }}</td>
              <td class="num">{{ d.cantidad }}</td>
              <td class="num">{{ d.costoUnitario.toFixed(2) }}</td>
              <td class="num">{{ d.subtotal.toFixed(2) }}</td>
            </tr>
          }
        </tbody>
        <tfoot>
          <tr>
            <td colspan="3" class="num"><strong>Total</strong></td>
            <td class="num"><strong>S/ {{ datos.compra.total.toFixed(2) }}</strong></td>
          </tr>
        </tfoot>
      </table>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-flat-button type="button" mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .datos { margin: 0 0 12px; opacity: 0.8; }
    .estado { padding: 8px 12px; border-radius: 8px; }
    .pendiente { background: #fff4e0; color: #8a5300; }
    .recibida { background: #e6f4ea; color: #1e6b34; }
    .rechazada { background: #fdecea; color: #a1281c; }
    .detalle { width: 100%; border-collapse: collapse; }
    .detalle th, .detalle td { padding: 6px 8px; border-bottom: 1px solid color-mix(in srgb, currentColor 14%, transparent); text-align: left; }
    .num { text-align: right !important; }
  `,
})
export class CompraDetalle {
  protected readonly datos = inject<DatosCompraDetalle>(MAT_DIALOG_DATA);
}
