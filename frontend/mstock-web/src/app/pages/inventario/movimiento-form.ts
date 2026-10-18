import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable, finalize } from 'rxjs';
import { InventarioApi } from '../../core/api/inventario.api';
import { Producto } from '../../core/models/catalogo';
import { Movimiento, Stock } from '../../core/models/inventario';
import { Notificador } from '../../core/notificador.service';
import { vacioANull } from '../../core/util/texto';

export type TipoOperacion = 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'MINIMO';

export interface DatosMovimiento {
  tipo: TipoOperacion;
  /** Productos activos del catálogo (para elegir el producto de una entrada nueva). */
  productos: Producto[];
  /** Si viene, el producto ya está elegido (acción desde una fila de la tabla). */
  stock: Stock | null;
  /** Nombre para mostrar del producto de la fila. */
  nombreProducto?: string;
}

const TEXTOS: Record<TipoOperacion, { titulo: string; cantidad: string; boton: string; ok: string }> = {
  ENTRADA: { titulo: 'Registrar entrada', cantidad: 'Cantidad que ingresa', boton: 'Registrar entrada', ok: 'Entrada registrada' },
  SALIDA: { titulo: 'Registrar salida', cantidad: 'Cantidad que sale', boton: 'Registrar salida', ok: 'Salida registrada' },
  AJUSTE: { titulo: 'Ajustar con conteo físico', cantidad: 'Cantidad real contada', boton: 'Ajustar stock', ok: 'Stock ajustado' },
  MINIMO: { titulo: 'Stock mínimo', cantidad: 'Stock mínimo', boton: 'Guardar', ok: 'Stock mínimo actualizado' },
};

/** Una salida no puede superar el stock actual (el servidor también lo valida). */
function noMasQueElStock(disponible: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = control.value as number | null;
    return valor !== null && valor > disponible ? { sinStock: { disponible } } : null;
  };
}

/** Entrada, salida, ajuste o cambio de stock mínimo, en una sola ventana. Cierra con true si se guardó. */
@Component({
  selector: 'app-movimiento-form',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ textos.titulo }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos">
        @if (datos.stock) {
          <p class="producto"><strong>{{ datos.nombreProducto }}</strong></p>
          <p class="info">
            Stock actual: <strong>{{ datos.stock.cantidad }}</strong> · Mínimo:
            <strong>{{ datos.stock.stockMinimo }}</strong>
          </p>
        } @else {
          <mat-form-field appearance="outline">
            <mat-label>Producto</mat-label>
            <mat-select formControlName="productoId">
              @for (p of datos.productos; track p.id) {
                <mat-option [value]="p.id">{{ p.codigo }} · {{ p.nombre }}</mat-option>
              }
            </mat-select>
            @if (form.controls.productoId.hasError('required')) {
              <mat-error>Elige un producto</mat-error>
            }
          </mat-form-field>
        }

        <mat-form-field appearance="outline">
          <mat-label>{{ textos.cantidad }}</mat-label>
          <input matInput type="number" step="1" formControlName="cantidad" autofocus />
          @if (form.controls.cantidad.hasError('required')) {
            <mat-error>La cantidad es obligatoria</mat-error>
          } @else if (form.controls.cantidad.hasError('sinStock')) {
            <mat-error>Stock insuficiente: hay {{ datos.stock?.cantidad }}</mat-error>
          } @else if (form.controls.cantidad.invalid) {
            <mat-error>{{ mensajeCantidad() }}</mat-error>
          }
        </mat-form-field>

        @if (datos.tipo !== 'MINIMO') {
          <mat-form-field appearance="outline">
            <mat-label>Motivo{{ datos.tipo === 'AJUSTE' ? '' : ' (opcional)' }}</mat-label>
            <input matInput formControlName="motivo" maxlength="200" />
            @if (datos.tipo === 'AJUSTE') {
              <mat-hint>Obligatorio: queda en el kardex</mat-hint>
            }
            @if (form.controls.motivo.hasError('required')) {
              <mat-error>El motivo es obligatorio</mat-error>
            }
          </mat-form-field>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" [mat-dialog-close]="false" [disabled]="guardando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">{{ textos.boton }}</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campos { display: flex; flex-direction: column; gap: 4px; min-width: min(440px, 80vw); }
    .producto { margin: 0; }
    .info { margin: 0 0 8px; opacity: 0.75; }
  `,
})
export class MovimientoForm {
  protected readonly datos = inject<DatosMovimiento>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<MovimientoForm, boolean>>(MatDialogRef);
  private readonly api = inject(InventarioApi);
  private readonly notificador = inject(Notificador);

  protected readonly textos = TEXTOS[this.datos.tipo];
  protected readonly guardando = signal(false);

  private readonly minimo = this.datos.tipo === 'ENTRADA' || this.datos.tipo === 'SALIDA' ? 1 : 0;
  protected readonly mensajeCantidad = computed(() =>
    this.minimo === 1
      ? 'Un número entero entre 1 y 1 000 000'
      : 'Un número entero entre 0 y 1 000 000',
  );

  protected readonly form = inject(FormBuilder).group({
    productoId: [this.datos.stock?.productoId ?? (null as number | null), Validators.required],
    cantidad: [
      this.datos.tipo === 'MINIMO' ? (this.datos.stock?.stockMinimo ?? null) : (null as number | null),
      [
        Validators.required,
        Validators.min(this.minimo),
        Validators.max(1_000_000),
        Validators.pattern(/^\d+$/),
        ...(this.datos.tipo === 'SALIDA' && this.datos.stock ? [noMasQueElStock(this.datos.stock.cantidad)] : []),
      ],
    ],
    motivo: [
      '',
      this.datos.tipo === 'AJUSTE'
        ? [Validators.required, Validators.maxLength(200)]
        : [Validators.maxLength(200)],
    ],
  });

  protected guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const productoId = v.productoId as number;
    const cantidad = v.cantidad as number;
    const motivo = vacioANull(v.motivo);

    let peticion: Observable<Movimiento | Stock>;
    switch (this.datos.tipo) {
      case 'ENTRADA':
        peticion = this.api.entrada({ productoId, cantidad, motivo });
        break;
      case 'SALIDA':
        peticion = this.api.salida({ productoId, cantidad, motivo });
        break;
      case 'AJUSTE':
        peticion = this.api.ajustar({ productoId, cantidadReal: cantidad, motivo: motivo ?? '' });
        break;
      case 'MINIMO':
        peticion = this.api.actualizarStockMinimo(productoId, cantidad);
        break;
    }

    this.guardando.set(true);
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.notificador.ok(this.textos.ok);
        this.ref.close(true);
      },
      error: (error: unknown) => this.notificador.error(error, 'No se pudo guardar la operación'),
    });
  }
}
