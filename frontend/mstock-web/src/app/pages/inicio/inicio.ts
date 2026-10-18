import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthApi } from '../../core/api/auth.api';
import { menuPara } from '../../core/auth/navegacion';
import { SesionService } from '../../core/auth/sesion.service';

type Conexion = 'verificando' | 'ok' | 'error';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, MatCardModule, MatIconModule, MatChipsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <h1>Hola, {{ usuario() }}</h1>
      <p class="sub">
        Rol: <strong>{{ rol() }}</strong>
        @switch (conexion()) {
          @case ('ok') {
            <mat-chip-set class="chip"><mat-chip highlighted>Conectado al gateway</mat-chip></mat-chip-set>
          }
          @case ('error') {
            <mat-chip-set class="chip"><mat-chip>Sin conexión con el gateway</mat-chip></mat-chip-set>
          }
        }
      </p>

      <div class="rejilla">
        @for (item of atajos(); track item.ruta) {
          <a [routerLink]="item.ruta" class="atajo">
            <mat-card appearance="outlined">
              <mat-card-content class="contenido">
                <mat-icon>{{ item.icono }}</mat-icon>
                <span>{{ item.etiqueta }}</span>
              </mat-card-content>
            </mat-card>
          </a>
        }
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1100px; margin: 0 auto; }
    .sub { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .rejilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 16px; margin-top: 16px; }
    .atajo { text-decoration: none; color: inherit; }
    .contenido { display: flex; align-items: center; gap: 12px; padding: 16px; }
  `,
})
export class Inicio {
  private readonly sesion = inject(SesionService);

  protected readonly usuario = this.sesion.usuario;
  protected readonly rol = this.sesion.rol;
  protected readonly atajos = computed(() =>
    menuPara(this.sesion.rol()).filter((item) => item.ruta !== '/inicio'),
  );

  /** Prueba real de integración: el gateway valida el token y el auth-service responde. */
  protected readonly conexion = toSignal(
    inject(AuthApi)
      .yo()
      .pipe(
        map((): Conexion => 'ok'),
        catchError(() => of<Conexion>('error')),
      ),
    { initialValue: 'verificando' as Conexion },
  );
}
