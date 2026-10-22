import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';
import { ComprasApi } from '../../core/api/compras.api';
import { Producto, Proveedor } from '../../core/models/catalogo';
import { Compra } from '../../core/models/compras';
import { Notificador } from '../../core/notificador.service';

export interface DatosCompraForm {
  proveedores: Proveedor[];
  productos: Producto[];
}

interface Linea {
  producto: Producto;
  cantidad: number;
}

const MAX_CANTIDAD = 1_000_000;

/**
 * Registrar una compra: proveedor + productos con su cantidad. El costo unitario lo toma el servidor del
 * precio de compra del catálogo; aquí se muestra solo como vista previa. Cierra con la compra creada.
 */
@Component({
  selector: 'app-compra-form',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Nueva compra</h2>
    <mat-dialog-content class="campos">
      <mat-form-field appearance="outline">
        <mat-label>Proveedor</mat-label>
        <mat-select [formControl]="proveedor">
          @for (p of datos.proveedores; track p.id) {
            <mat-option [value]="p.id">{{ p.razonSocial }} · {{ p.ruc }}</mat-option>
          }
        </mat-select>
        @if (proveedor.hasError('required') && proveedor.touched) {
          <mat-error>Elige un proveedor</mat-error>
        }
      </mat-form-field>

      <div class="agregar">
        <mat-form-field appearance="outline" class="producto">
          <mat-label>Producto</mat-label>
          <mat-select [formControl]="productoElegido">
            @for (p of disponibles(); track p.id) {
              <mat-option [value]="p.id">{{ p.codigo }} · {{ p.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="cant">
          <mat-label>Cantidad</mat-label>
          <input matInput type="number" min="1" [max]="maxCantidad" [formControl]="cantidadNueva" (keydown.enter)="agregar($event)" />
        </mat-form-field>
        <button mat-stroked-button type="button" (click)="agregar()" [disabled]="!productoElegido.value">
          <mat-icon>add</mat-icon> Agregar
        </button>
      </div>

      <table class="detalle">
        <thead>
          <tr><th>Producto</th><th class="num">Cant.</th><th class="num">Costo</th><th class="num">Subtotal</th><th></th></tr>
        </thead>
        <tbody>
          @for (l of lineas(); track l.producto.id) {
            <tr>
              <td>{{ l.producto.nombre }}<br /><small>{{ l.producto.codigo }}</small></td>
              <td class="num">
                <input
                  class="cantidad"
                  type="number"
                  min="1"
                  [max]="maxCantidad"
                  [value]="l.cantidad"
                  aria-label="Cantidad"
                  (change)="cambiar(l, $any($event.target))"
                />
              </td>
              <td class="num">{{ l.producto.precioCompra.toFixed(2) }}</td>
              <td class="num">{{ subtotal(l).toFixed(2) }}</td>
              <td class="num">
                <button mat-icon-button type="button" aria-label="Quitar" (click)="quitar(l)"><mat-icon>delete_outline</mat-icon></button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="vacio">Agrega al menos un producto</td></tr>
          }
        </tbody>
        <tfoot>
          <tr><td colspan="3" class="num"><strong>Total estimado</strong></td><td class="num"><strong>S/ {{ total().toFixed(2) }}</strong></td><td></td></tr>
        </tfoot>
      </table>
      <p class="nota">El costo sale del precio de compra del catálogo. El stock se suma cuando el inventario confirma la compra.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="null" [disabled]="guardando()">Cancelar</button>
      <button mat-flat-button type="button" (click)="guardar()" [disabled]="guardando()">Registrar compra</button>
    </mat-dialog-actions>
  `,
  styles: `
    .campos { display: flex; flex-direction: column; gap: 4px; min-width: min(640px, 85vw); }
    .agregar { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; }
    .producto { flex: 1 1 260px; }
    .cant { width: 120px; }
    .agregar button { margin-top: 6px; }
    .detalle { width: 100%; border-collapse: collapse; }
    .detalle th, .detalle td { padding: 6px 8px; border-bottom: 1px solid color-mix(in srgb, currentColor 14%, transparent); text-align: left; }
    .num { text-align: right !important; }
    small { opacity: 0.65; }
    .vacio { text-align: center !important; opacity: 0.7; padding: 16px; }
    .nota { font-size: 12px; opacity: 0.7; margin: 8px 0 0; }
    .cantidad {
      width: 84px; text-align: right; font: inherit; padding: 4px 6px; border-radius: 8px;
      color: inherit; background: transparent; border: 1px solid color-mix(in srgb, currentColor 30%, transparent);
    }
  `,
})
export class CompraForm {
  protected readonly datos = inject<DatosCompraForm>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<CompraForm, Compra | null>>(MatDialogRef);
  private readonly api = inject(ComprasApi);
  private readonly notificador = inject(Notificador);

  protected readonly maxCantidad = MAX_CANTIDAD;
  protected readonly proveedor = new FormControl<number | null>(null, Validators.required);
  protected readonly productoElegido = new FormControl<number | null>(null);
  protected readonly cantidadNueva = new FormControl(1, { nonNullable: true });
  protected readonly lineas = signal<Linea[]>([]);
  protected readonly guardando = signal(false);

  protected readonly disponibles = computed(() => {
    const usados = new Set(this.lineas().map((l) => l.producto.id));
    return this.datos.productos.filter((p) => !usados.has(p.id));
  });
  protected readonly total = computed(
    () => this.lineas().reduce((suma, l) => suma + Math.round(l.producto.precioCompra * 100) * l.cantidad, 0) / 100,
  );

  protected subtotal(l: Linea): number {
    return (Math.round(l.producto.precioCompra * 100) * l.cantidad) / 100;
  }

  protected agregar(evento?: Event): void {
    evento?.preventDefault();
    const producto = this.datos.productos.find((p) => p.id === this.productoElegido.value);
    const cantidad = Math.floor(Number(this.cantidadNueva.value));
    if (!producto) {
      return;
    }
    if (!Number.isFinite(cantidad) || cantidad < 1 || cantidad > MAX_CANTIDAD) {
      this.notificador.aviso(`La cantidad debe estar entre 1 y ${MAX_CANTIDAD}`);
      return;
    }
    this.lineas.update((ls) => [...ls, { producto, cantidad }]);
    this.productoElegido.setValue(null);
    this.cantidadNueva.setValue(1);
  }

  protected cambiar(linea: Linea, campo: HTMLInputElement): void {
    const valor = Math.floor(campo.valueAsNumber);
    if (!Number.isFinite(valor) || valor < 1 || valor > MAX_CANTIDAD) {
      campo.value = String(linea.cantidad);
      return;
    }
    this.lineas.update((ls) => ls.map((l) => (l.producto.id === linea.producto.id ? { ...l, cantidad: valor } : l)));
  }

  protected quitar(linea: Linea): void {
    this.lineas.update((ls) => ls.filter((l) => l.producto.id !== linea.producto.id));
  }

  protected guardar(): void {
    this.proveedor.markAsTouched();
    if (this.proveedor.invalid || this.guardando()) {
      return;
    }
    if (this.lineas().length === 0) {
      this.notificador.aviso('Agrega al menos un producto');
      return;
    }
    this.guardando.set(true);
    this.api
      .registrar({
        proveedorId: this.proveedor.value as number,
        items: this.lineas().map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad })),
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (compra) => this.ref.close(compra),
        error: (error: unknown) => this.notificador.error(error, 'No se pudo registrar la compra'),
      });
  }
}
