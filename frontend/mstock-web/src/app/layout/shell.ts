import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map, timer } from 'rxjs';
import { ContadorNotificaciones } from '../core/contador-notificaciones';
import { menuPara } from '../core/auth/navegacion';
import { SesionService } from '../core/auth/sesion.service';

/** Marco de la aplicación: barra superior, menú lateral según el rol y la campana de notificaciones. */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-toolbar class="barra">
      <button mat-icon-button type="button" aria-label="Mostrar u ocultar el menú"
              (click)="menuAbierto.set(!menuAbierto())">
        <mat-icon>menu</mat-icon>
      </button>
      <span class="marca">MSTOCK</span>
      <span class="espacio"></span>

      @if (muestraCampana()) {
        <a mat-icon-button routerLink="/notificaciones" aria-label="Notificaciones"
           matTooltip="Notificaciones">
          <mat-icon [matBadge]="noLeidas() > 0 ? noLeidas() : null" matBadgeColor="warn"
                    matBadgeSize="small" aria-hidden="false">notifications</mat-icon>
        </a>
      }
      @if (!esMovil()) {
        <span class="usuario">{{ usuario() }} · {{ rol() }}</span>
      }
      <button mat-icon-button type="button" matTooltip="Cerrar sesión" aria-label="Cerrar sesión"
              (click)="cerrarSesion()">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>

    <mat-sidenav-container class="contenedor">
      <mat-sidenav [mode]="esMovil() ? 'over' : 'side'" [opened]="menuAbierto()"
                   (openedChange)="menuAbierto.set($event)" class="lateral">
        <mat-nav-list>
          @for (item of menu(); track item.ruta) {
            <a mat-list-item [routerLink]="item.ruta" routerLinkActive="activo"
               (click)="alNavegar()">
              <mat-icon matListItemIcon>{{ item.icono }}</mat-icon>
              <span matListItemTitle>{{ item.etiqueta }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `
    :host { display: flex; flex-direction: column; height: 100vh; }
    .barra { background: var(--mat-sys-primary, #3f51b5); color: var(--mat-sys-on-primary, #fff); flex: 0 0 auto; }
    .marca { font-weight: 600; letter-spacing: 0.08em; margin-left: 4px; }
    .espacio { flex: 1 1 auto; }
    .usuario { margin: 0 8px; font-size: 14px; }
    .contenedor { flex: 1 1 auto; }
    .lateral { width: 240px; }
    .activo { font-weight: 600; background: color-mix(in srgb, currentColor 10%, transparent); }
  `,
})
export class Shell {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly contador = inject(ContadorNotificaciones);
  private readonly pantallas = inject(BreakpointObserver);

  protected readonly usuario = this.sesion.usuario;
  protected readonly rol = this.sesion.rol;
  protected readonly menu = computed(() => menuPara(this.sesion.rol()));
  protected readonly muestraCampana = computed(() => this.sesion.tieneRol(['ADMIN', 'ALMACENERO']));

  protected readonly esMovil = toSignal(
    this.pantallas.observe(Breakpoints.Handset).pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );
  protected readonly menuAbierto = signal(!this.esMovil());

  /** Notificaciones sin leer, consultadas cada 30 segundos. Si falla la consulta, no se muestra globo. */
  protected readonly noLeidas = this.contador.noLeidas;

  constructor() {
    timer(0, 30_000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (this.muestraCampana()) {
          this.contador.refrescar();
        }
      });
  }

  protected alNavegar(): void {
    if (this.esMovil()) {
      this.menuAbierto.set(false);
    }
  }

  protected cerrarSesion(): void {
    this.sesion.cerrar();
    void this.router.navigate(['/login']);
  }
}
