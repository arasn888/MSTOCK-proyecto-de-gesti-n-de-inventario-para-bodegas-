import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApi } from '../../core/api/auth.api';
import { SesionService } from '../../core/auth/sesion.service';
import { Notificador } from '../../core/notificador.service';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="pantalla">
      <mat-card class="tarjeta">
        <mat-card-header>
          <mat-card-title>MSTOCK</mat-card-title>
          <mat-card-subtitle>Inventario y ventas para bodegas</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          @if (sesionExpirada) {
            <p class="aviso" role="alert">Tu sesión expiró. Inicia sesión de nuevo.</p>
          }
          <form [formGroup]="form" (ngSubmit)="entrar()" class="formulario">
            <mat-form-field appearance="outline">
              <mat-label>Usuario</mat-label>
              <input matInput formControlName="username" autocomplete="username" autofocus />
              @if (form.controls.username.hasError('required')) {
                <mat-error>El usuario es obligatorio</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Contraseña</mat-label>
              <input matInput formControlName="password" autocomplete="current-password"
                     [type]="verClave() ? 'text' : 'password'" />
              <button mat-icon-button matSuffix type="button"
                      [attr.aria-label]="verClave() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                      (click)="verClave.set(!verClave())">
                <mat-icon>{{ verClave() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.controls.password.hasError('required')) {
                <mat-error>La contraseña es obligatoria</mat-error>
              }
            </mat-form-field>

            <button mat-flat-button type="submit" [disabled]="cargando()">
              @if (cargando()) {
                <mat-spinner diameter="20" />
              } @else {
                Ingresar
              }
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: `
    .pantalla { min-height: 100vh; display: grid; place-items: center; padding: 16px; }
    .tarjeta { width: 100%; max-width: 380px; padding: 8px; }
    .formulario { display: flex; flex-direction: column; gap: 4px; margin-top: 16px; }
    .aviso { color: var(--mat-sys-error, #b3261e); margin: 8px 0 0; }
    button[type='submit'] { height: 44px; }
  `,
})
export class Login {
  private readonly auth = inject(AuthApi);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly notificador = inject(Notificador);
  private readonly parametros = inject(ActivatedRoute).snapshot.queryParamMap;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly cargando = signal(false);
  protected readonly verClave = signal(false);
  protected readonly sesionExpirada = this.parametros.get('expirada') === '1';

  protected entrar(): void {
    if (this.form.invalid || this.cargando()) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando.set(true);
    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (respuesta) => {
          this.sesion.iniciar(respuesta);
          void this.router.navigateByUrl(this.destino());
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo iniciar sesión'),
      });
  }

  /** A donde volver tras el login. Solo rutas internas: evita redirigir a otro sitio por un enlace manipulado. */
  private destino(): string {
    const volverA = this.parametros.get('volverA');
    const interna = volverA !== null && volverA.startsWith('/') && !volverA.startsWith('//');
    return interna && !volverA.startsWith('/login') ? volverA : '/inicio';
  }
}
