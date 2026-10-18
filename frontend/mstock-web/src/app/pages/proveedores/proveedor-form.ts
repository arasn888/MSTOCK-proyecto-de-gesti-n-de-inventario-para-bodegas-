import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { ProveedoresApi } from '../../core/api/proveedores.api';
import { Proveedor, ProveedorRequest } from '../../core/models/catalogo';
import { Notificador } from '../../core/notificador.service';
import { vacioANull } from '../../core/util/texto';

/** Crear (data = null) o editar (data = el proveedor) un proveedor. Cierra con true si se guardó. */
@Component({
  selector: 'app-proveedor-form',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatButtonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ proveedor ? 'Editar proveedor' : 'Nuevo proveedor' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos">
        <mat-form-field appearance="outline">
          <mat-label>RUC</mat-label>
          <input matInput formControlName="ruc" inputmode="numeric" maxlength="11" autofocus />
          <mat-hint>11 dígitos; empieza con 10, 15, 16, 17 o 20</mat-hint>
          @if (form.controls.ruc.hasError('required')) {
            <mat-error>El RUC es obligatorio</mat-error>
          } @else if (form.controls.ruc.hasError('pattern')) {
            <mat-error>Debe tener 11 dígitos y empezar con 10, 15, 16, 17 o 20</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Razón social</mat-label>
          <input matInput formControlName="razonSocial" maxlength="150" />
          @if (form.controls.razonSocial.hasError('required')) {
            <mat-error>La razón social es obligatoria</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Teléfono (opcional)</mat-label>
          <input matInput formControlName="telefono" maxlength="20" />
          @if (form.controls.telefono.hasError('pattern')) {
            <mat-error>El teléfono no tiene un formato válido</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Correo (opcional)</mat-label>
          <input matInput type="email" formControlName="email" maxlength="120" />
          @if (form.controls.email.hasError('email')) {
            <mat-error>El correo no tiene un formato válido</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Dirección (opcional)</mat-label>
          <input matInput formControlName="direccion" maxlength="200" />
        </mat-form-field>
        <mat-slide-toggle formControlName="activo">Activo</mat-slide-toggle>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" [mat-dialog-close]="false" [disabled]="guardando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campos { display: flex; flex-direction: column; gap: 4px; min-width: min(440px, 80vw); }
  `,
})
export class ProveedorForm {
  protected readonly proveedor = inject<Proveedor | null>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<ProveedorForm, boolean>>(MatDialogRef);
  private readonly api = inject(ProveedoresApi);
  private readonly notificador = inject(Notificador);

  protected readonly guardando = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    ruc: [
      this.proveedor?.ruc ?? '',
      [Validators.required, Validators.pattern(/^(10|15|16|17|20)\d{9}$/)],
    ],
    razonSocial: [this.proveedor?.razonSocial ?? '', [Validators.required, Validators.maxLength(150)]],
    telefono: [this.proveedor?.telefono ?? '', Validators.pattern(/^$|^[0-9+() -]{6,20}$/)],
    email: [this.proveedor?.email ?? '', [Validators.email, Validators.maxLength(120)]],
    direccion: [this.proveedor?.direccion ?? '', Validators.maxLength(200)],
    activo: [this.proveedor?.activo ?? true],
  });

  protected guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const datos: ProveedorRequest = {
      ruc: v.ruc.trim(),
      razonSocial: v.razonSocial.trim(),
      telefono: vacioANull(v.telefono),
      email: vacioANull(v.email),
      direccion: vacioANull(v.direccion),
      activo: v.activo,
    };
    this.guardando.set(true);
    const peticion = this.proveedor
      ? this.api.actualizar(this.proveedor.id, datos)
      : this.api.crear(datos);
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.notificador.ok(this.proveedor ? 'Proveedor actualizado' : 'Proveedor creado');
        this.ref.close(true);
      },
      error: (error: unknown) => this.notificador.error(error, 'No se pudo guardar el proveedor'),
    });
  }
}
