import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { ClientesApi } from '../../core/api/clientes.api';
import { Cliente, ClienteRequest } from '../../core/models/ventas';
import { Notificador } from '../../core/notificador.service';
import { vacioANull } from '../../core/util/texto';

/** Crear (data = null) o editar (data = el cliente) un cliente. Cierra con true si se guardó. */
@Component({
  selector: 'app-cliente-form',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ cliente ? 'Editar cliente' : 'Nuevo cliente' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos">
        <mat-form-field appearance="outline">
          <mat-label>Nombre o razón social</mat-label>
          <input matInput formControlName="nombres" maxlength="120" autofocus />
          @if (form.controls.nombres.hasError('required')) {
            <mat-error>El nombre es obligatorio</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>DNI o RUC (opcional)</mat-label>
          <input matInput formControlName="documento" inputmode="numeric" maxlength="11" />
          <mat-hint>8 dígitos (DNI) u 11 dígitos (RUC)</mat-hint>
          @if (form.controls.documento.hasError('pattern')) {
            <mat-error>Debe tener 8 dígitos (DNI) u 11 dígitos (RUC)</mat-error>
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
export class ClienteForm {
  protected readonly cliente = inject<Cliente | null>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<ClienteForm, boolean>>(MatDialogRef);
  private readonly api = inject(ClientesApi);
  private readonly notificador = inject(Notificador);

  protected readonly guardando = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    nombres: [this.cliente?.nombres ?? '', [Validators.required, Validators.maxLength(120)]],
    documento: [this.cliente?.documento ?? '', Validators.pattern(/^(\d{8}|\d{11})?$/)],
    telefono: [this.cliente?.telefono ?? '', Validators.pattern(/^$|^[0-9+() -]{6,20}$/)],
    email: [this.cliente?.email ?? '', [Validators.email, Validators.maxLength(120)]],
  });

  protected guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const datos: ClienteRequest = {
      nombres: v.nombres.trim(),
      documento: vacioANull(v.documento),
      telefono: vacioANull(v.telefono),
      email: vacioANull(v.email),
    };
    this.guardando.set(true);
    const peticion = this.cliente ? this.api.actualizar(this.cliente.id, datos) : this.api.crear(datos);
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.notificador.ok(this.cliente ? 'Cliente actualizado' : 'Cliente creado');
        this.ref.close(true);
      },
      error: (error: unknown) => this.notificador.error(error, 'No se pudo guardar el cliente'),
    });
  }
}
