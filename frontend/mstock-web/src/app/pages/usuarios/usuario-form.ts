import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { UsuariosApi } from '../../core/api/usuarios.api';
import { SesionService } from '../../core/auth/sesion.service';
import { ROLES, Usuario, UsuarioUpdateRequest } from '../../core/models/auth';
import { Notificador } from '../../core/notificador.service';

/** Crear (data = null) o editar (data = el usuario). Cierra con true si se guardó. */
@Component({
  selector: 'app-usuario-form',
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
    <h2 mat-dialog-title>{{ usuario ? 'Editar usuario' : 'Nuevo usuario' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos">
        <mat-form-field appearance="outline">
          <mat-label>Usuario</mat-label>
          <input matInput formControlName="username" maxlength="50" autocomplete="off" />
          <mat-hint>Entre 4 y 50: letras, números, punto, guion y guion bajo</mat-hint>
          @if (form.controls.username.invalid && form.controls.username.touched) {
            <mat-error>Usuario no válido (4 a 50 caracteres; solo letras, números, . _ -)</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>{{ usuario ? 'Nueva contraseña (opcional)' : 'Contraseña' }}</mat-label>
          <input matInput type="password" formControlName="password" maxlength="72" autocomplete="new-password" />
          <mat-hint>{{ usuario ? 'Déjala vacía para no cambiarla; ' : '' }}entre 8 y 72 caracteres</mat-hint>
          @if (form.controls.password.invalid && form.controls.password.touched) {
            <mat-error>La contraseña debe tener entre 8 y 72 caracteres</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nombres</mat-label>
          <input matInput formControlName="nombres" maxlength="100" />
          @if (form.controls.nombres.invalid && form.controls.nombres.touched) {
            <mat-error>Los nombres son obligatorios</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Apellidos</mat-label>
          <input matInput formControlName="apellidos" maxlength="100" />
          @if (form.controls.apellidos.invalid && form.controls.apellidos.touched) {
            <mat-error>Los apellidos son obligatorios</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Correo</mat-label>
          <input matInput type="email" formControlName="email" maxlength="120" />
          @if (form.controls.email.invalid && form.controls.email.touched) {
            <mat-error>Ingresa un correo válido</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Rol</mat-label>
          <mat-select formControlName="rol">
            @for (r of roles; track r) {
              <mat-option [value]="r">{{ r }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        @if (usuario) {
          <mat-slide-toggle formControlName="activo">Activo</mat-slide-toggle>
          @if (esYo) {
            <p class="nota">No puedes desactivarte ni quitarte el rol de administrador a ti mismo.</p>
          }
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" [mat-dialog-close]="false" [disabled]="guardando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campos { display: flex; flex-direction: column; gap: 4px; min-width: min(440px, 80vw); }
    .nota { font-size: 12px; opacity: 0.7; margin: 4px 0 0; }
  `,
})
export class UsuarioForm {
  protected readonly usuario = inject<Usuario | null>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<UsuarioForm, boolean>>(MatDialogRef);
  private readonly api = inject(UsuariosApi);
  private readonly notificador = inject(Notificador);

  protected readonly roles = ROLES;
  protected readonly esYo = this.usuario !== null && this.usuario.username === inject(SesionService).usuario();
  protected readonly guardando = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: [
      { value: this.usuario?.username ?? '', disabled: this.usuario !== null },
      [Validators.required, Validators.minLength(4), Validators.maxLength(50), Validators.pattern(/^[a-zA-Z0-9._-]+$/)],
    ],
    password: [
      '',
      this.usuario
        ? [Validators.minLength(8), Validators.maxLength(72)]
        : [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    ],
    nombres: [this.usuario?.nombres ?? '', [Validators.required, Validators.maxLength(100)]],
    apellidos: [this.usuario?.apellidos ?? '', [Validators.required, Validators.maxLength(100)]],
    email: [this.usuario?.email ?? '', [Validators.required, Validators.email, Validators.maxLength(120)]],
    rol: [this.usuario?.rol ?? ROLES[1], Validators.required],
    activo: [this.usuario?.activo ?? true],
  });

  constructor() {
    if (this.esYo) {
      this.form.controls.rol.disable();
      this.form.controls.activo.disable();
    }
  }

  protected guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    const peticion = this.usuario
      ? this.api.actualizar(this.usuario.id, {
          nombres: v.nombres.trim(),
          apellidos: v.apellidos.trim(),
          email: v.email.trim(),
          rol: v.rol,
          activo: v.activo,
          ...(v.password.trim() !== '' ? { password: v.password } : {}),
        } satisfies UsuarioUpdateRequest)
      : this.api.crear({
          username: v.username.trim(),
          password: v.password,
          nombres: v.nombres.trim(),
          apellidos: v.apellidos.trim(),
          email: v.email.trim(),
          rol: v.rol,
        });
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.notificador.ok(this.usuario ? 'Usuario actualizado' : 'Usuario creado');
        this.ref.close(true);
      },
      error: (error: unknown) => this.notificador.error(error, 'No se pudo guardar el usuario'),
    });
  }
}
