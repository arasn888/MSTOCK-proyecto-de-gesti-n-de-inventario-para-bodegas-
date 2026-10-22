import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize } from 'rxjs';
import { VentasApi } from '../../core/api/ventas.api';
import { SesionService } from '../../core/auth/sesion.service';
import { abrirPdf } from '../../core/http/descargas';
import { Venta } from '../../core/models/ventas';
import { Notificador } from '../../core/notificador.service';

export interface DatosVentaDetalle {
  ventaId: number;
}

/** Detalle de una venta, con boleta y (solo ADMIN) anulación. Cierra con true si la venta se anuló. */
@Component({
  selector: 'app-venta-detalle',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Venta {{ venta()?.numero ?? '' }}</h2>
    <mat-dialog-content>
      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      @if (venta(); as v) {
        <p class="datos">
          {{ v.fecha | date: 'dd/MM/yyyy HH:mm' }} · Cajero: {{ v.usuario }} ·
          Cliente: {{ v.clienteNombre ?? 'Consumidor final' }}
        </p>
        @if (v.estado === 'ANULADA') {
          <p class="anulada" role="status">
            <strong>ANULADA</strong>
            @if (v.fechaAnulacion) { el {{ v.fechaAnulacion | date: 'dd/MM/yyyy HH:mm' }} }
            @if (v.usuarioAnulacion) { por {{ v.usuarioAnulacion }} }
            @if (v.motivoAnulacion) { — {{ v.motivoAnulacion }} }
          </p>
        }
        <table class="detalle">
          <thead>
            <tr><th>Producto</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Subtotal</th></tr>
          </thead>
          <tbody>
            @for (d of v.detalle; track d.productoId) {
              <tr>
                <td>{{ d.nombre }}<br /><small>{{ d.codigo }}</small></td>
                <td class="num">{{ d.cantidad }}</td>
                <td class="num">{{ d.precioUnitario.toFixed(2) }}</td>
                <td class="num">{{ d.subtotal.toFixed(2) }}</td>
              </tr>
            }
          </tbody>
          <tfoot>
            <tr><td colspan="3" class="num"><strong>Total</strong></td><td class="num"><strong>S/ {{ v.total.toFixed(2) }}</strong></td></tr>
          </tfoot>
        </table>

        @if (anulando()) {
          <mat-form-field appearance="outline" class="motivo">
            <mat-label>Motivo de la anulación</mat-label>
            <textarea matInput rows="2" maxlength="200" [formControl]="motivo"></textarea>
            <mat-hint>El stock de los productos se devuelve al inventario</mat-hint>
            @if (motivo.hasError('required') && motivo.touched) {
              <mat-error>El motivo es obligatorio</mat-error>
            }
          </mat-form-field>
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      @if (venta(); as v) {
        <button mat-button type="button" (click)="boleta(v)" [disabled]="trabajando()">Ver boleta</button>
        @if (puedeAnular && v.estado === 'COMPLETADA') {
          @if (anulando()) {
            <button mat-button type="button" (click)="anulando.set(false)" [disabled]="trabajando()">Cancelar</button>
            <button mat-flat-button type="button" (click)="confirmarAnulacion(v)" [disabled]="trabajando()">
              Confirmar anulación
            </button>
          } @else {
            <button mat-button type="button" (click)="anulando.set(true)" [disabled]="trabajando()">Anular venta</button>
          }
        }
      }
      <button mat-flat-button type="button" [mat-dialog-close]="anulada()" [disabled]="trabajando()">Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .datos { margin: 0 0 12px; opacity: 0.8; }
    .anulada { padding: 8px 12px; border-radius: 8px; background: #fdecea; color: #a1281c; }
    .detalle { width: 100%; border-collapse: collapse; }
    .detalle th, .detalle td { padding: 6px 8px; border-bottom: 1px solid color-mix(in srgb, currentColor 14%, transparent); text-align: left; }
    .num { text-align: right !important; }
    small { opacity: 0.65; }
    .motivo { width: 100%; margin-top: 16px; }
  `,
})
export class VentaDetalle {
  private readonly datos = inject<DatosVentaDetalle>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<VentaDetalle, boolean>);
  private readonly api = inject(VentasApi);
  private readonly notificador = inject(Notificador);

  protected readonly puedeAnular = inject(SesionService).tieneRol(['ADMIN']);
  protected readonly venta = signal<Venta | null>(null);
  protected readonly cargando = signal(true);
  protected readonly trabajando = signal(false);
  protected readonly anulando = signal(false);
  protected readonly anulada = signal(false);
  protected readonly motivo = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] });

  constructor() {
    this.api
      .obtener(this.datos.ventaId)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (v) => this.venta.set(v),
        error: (error: unknown) => {
          this.notificador.error(error, 'No se pudo cargar la venta');
          this.ref.close(false);
        },
      });
  }

  protected boleta(v: Venta): void {
    this.trabajando.set(true);
    this.api
      .boleta(v.id)
      .pipe(finalize(() => this.trabajando.set(false)))
      .subscribe({
        next: (pdf) => abrirPdf(pdf, `boleta-${v.numero}.pdf`),
        error: (error: unknown) => this.notificador.error(error, 'No se pudo abrir la boleta'),
      });
  }

  protected confirmarAnulacion(v: Venta): void {
    this.motivo.markAsTouched();
    const motivo = this.motivo.value.trim();
    if (motivo === '' || this.trabajando()) {
      return;
    }
    this.trabajando.set(true);
    this.api
      .anular(v.id, motivo)
      .pipe(finalize(() => this.trabajando.set(false)))
      .subscribe({
        next: (actualizada) => {
          this.venta.set(actualizada);
          this.anulando.set(false);
          this.anulada.set(true);
          this.notificador.ok(`Venta ${actualizada.numero} anulada y stock devuelto`);
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo anular la venta'),
      });
  }
}
