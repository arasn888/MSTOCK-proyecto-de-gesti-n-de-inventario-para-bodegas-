import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { ProductosApi } from '../../core/api/productos.api';
import { Categoria, Producto, ProductoRequest, Proveedor } from '../../core/models/catalogo';
import { Notificador } from '../../core/notificador.service';
import { vacioANull } from '../../core/util/texto';

export interface DatosProductoForm {
  /** null = producto nuevo. */
  producto: Producto | null;
  categorias: Categoria[];
  proveedores: Proveedor[];
}

/** Hasta 2 decimales y 8 enteros, como valida el servidor. */
const PRECIO = Validators.pattern(/^\d{1,8}(\.\d{1,2})?$/);

/** El servidor rechaza vender por debajo del costo; se avisa antes de enviar. */
const ventaNoMenorQueCompra: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const compra = grupo.get('precioCompra')?.value as number | null;
  const venta = grupo.get('precioVenta')?.value as number | null;
  return compra !== null && venta !== null && venta < compra ? { ventaMenorQueCompra: true } : null;
};

/** Crear o editar un producto. Cierra con true si se guardó. */
@Component({
  selector: 'app-producto-form',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatButtonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ producto ? 'Editar producto' : 'Nuevo producto' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos">
        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Código</mat-label>
            <input matInput formControlName="codigo" maxlength="30" autofocus />
            <mat-hint>Se guarda en mayúsculas</mat-hint>
            @if (form.controls.codigo.hasError('required')) {
              <mat-error>El código es obligatorio</mat-error>
            } @else if (form.controls.codigo.hasError('pattern')) {
              <mat-error>Solo letras, números, punto, guion y guion bajo</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Unidad de medida</mat-label>
            <input matInput formControlName="unidadMedida" maxlength="20" placeholder="UNIDAD" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" maxlength="120" />
          @if (form.controls.nombre.hasError('required')) {
            <mat-error>El nombre es obligatorio</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Descripción (opcional)</mat-label>
          <textarea matInput formControlName="descripcion" rows="2" maxlength="255"></textarea>
        </mat-form-field>

        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Precio de compra (S/)</mat-label>
            <input matInput type="number" step="0.01" min="0" formControlName="precioCompra" />
            @if (form.controls.precioCompra.hasError('required')) {
              <mat-error>El precio de compra es obligatorio</mat-error>
            } @else if (form.controls.precioCompra.invalid) {
              <mat-error>Máximo 2 decimales y no negativo</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Precio de venta (S/)</mat-label>
            <input matInput type="number" step="0.01" min="0.01" formControlName="precioVenta" />
            @if (form.controls.precioVenta.hasError('required')) {
              <mat-error>El precio de venta es obligatorio</mat-error>
            } @else if (form.controls.precioVenta.invalid) {
              <mat-error>Mayor a cero y máximo 2 decimales</mat-error>
            }
          </mat-form-field>
        </div>
        @if (form.hasError('ventaMenorQueCompra')) {
          <p class="error" role="alert">El precio de venta no puede ser menor al precio de compra.</p>
        } @else if (margen() !== null) {
          <p class="margen">Margen por unidad: S/ {{ margen()!.toFixed(2) }}</p>
        }

        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Categoría</mat-label>
            <mat-select formControlName="categoriaId">
              @for (c of categorias(); track c.id) {
                <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
              }
            </mat-select>
            @if (form.controls.categoriaId.hasError('required')) {
              <mat-error>La categoría es obligatoria</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Proveedor (opcional)</mat-label>
            <mat-select formControlName="proveedorId">
              <mat-option [value]="null">Sin proveedor</mat-option>
              @for (p of proveedores(); track p.id) {
                <mat-option [value]="p.id">{{ p.razonSocial }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        </div>

        <mat-slide-toggle formControlName="activo">Activo (se puede vender y comprar)</mat-slide-toggle>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" [mat-dialog-close]="false" [disabled]="guardando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campos { display: flex; flex-direction: column; gap: 4px; min-width: min(560px, 80vw); }
    .fila { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    @media (max-width: 600px) { .fila { grid-template-columns: 1fr; } }
    .error { color: var(--mat-sys-error, #b3261e); margin: 0 0 8px; }
    .margen { opacity: 0.7; margin: 0 0 8px; }
  `,
})
export class ProductoForm {
  private readonly datos = inject<DatosProductoForm>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<ProductoForm, boolean>>(MatDialogRef);
  private readonly api = inject(ProductosApi);
  private readonly notificador = inject(Notificador);

  protected readonly producto = this.datos.producto;
  protected readonly guardando = signal(false);

  /** Solo activas (el servidor rechaza las inactivas), más la actual si el producto ya la tenía. */
  protected readonly categorias = computed(() =>
    this.datos.categorias.filter((c) => c.activo || c.id === this.producto?.categoriaId),
  );
  protected readonly proveedores = computed(() =>
    this.datos.proveedores.filter((p) => p.activo || p.id === this.producto?.proveedorId),
  );

  protected readonly form = inject(FormBuilder).group(
    {
      codigo: [
        this.producto?.codigo ?? '',
        [Validators.required, Validators.maxLength(30), Validators.pattern(/^[A-Za-z0-9._-]+$/)],
      ],
      nombre: [this.producto?.nombre ?? '', [Validators.required, Validators.maxLength(120)]],
      descripcion: [this.producto?.descripcion ?? '', Validators.maxLength(255)],
      precioCompra: [
        this.producto?.precioCompra ?? (null as number | null),
        [Validators.required, Validators.min(0), PRECIO],
      ],
      precioVenta: [
        this.producto?.precioVenta ?? (null as number | null),
        [Validators.required, Validators.min(0.01), PRECIO],
      ],
      unidadMedida: [this.producto?.unidadMedida ?? '', Validators.maxLength(20)],
      categoriaId: [this.producto?.categoriaId ?? (null as number | null), Validators.required],
      proveedorId: [this.producto?.proveedorId ?? (null as number | null)],
      activo: [this.producto?.activo ?? true],
    },
    { validators: ventaNoMenorQueCompra },
  );

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.value });
  protected readonly margen = computed(() => {
    const { precioCompra, precioVenta } = this.valores();
    return typeof precioCompra === 'number' && typeof precioVenta === 'number' && precioVenta >= precioCompra
      ? precioVenta - precioCompra
      : null;
  });

  protected guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const datos: ProductoRequest = {
      codigo: (v.codigo ?? '').trim(),
      nombre: (v.nombre ?? '').trim(),
      descripcion: vacioANull(v.descripcion),
      precioCompra: v.precioCompra as number,
      precioVenta: v.precioVenta as number,
      unidadMedida: vacioANull(v.unidadMedida),
      categoriaId: v.categoriaId as number,
      proveedorId: v.proveedorId,
      activo: v.activo ?? true,
    };
    this.guardando.set(true);
    const peticion = this.producto ? this.api.actualizar(this.producto.id, datos) : this.api.crear(datos);
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.notificador.ok(this.producto ? 'Producto actualizado' : 'Producto creado');
        this.ref.close(true);
      },
      error: (error: unknown) => this.notificador.error(error, 'No se pudo guardar el producto'),
    });
  }
}
