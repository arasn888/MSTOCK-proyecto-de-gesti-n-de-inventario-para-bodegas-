import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, finalize, switchMap } from 'rxjs';
import { UsuariosApi } from '../../core/api/usuarios.api';
import { SesionService } from '../../core/auth/sesion.service';
import { Usuario } from '../../core/models/auth';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { Confirmador } from '../../shared/confirmar';
import { UsuarioForm } from './usuario-form';

/** Gestión de usuarios (solo ADMIN). */
@Component({
  selector: 'app-usuarios',
  imports: [
    CabeceraPagina,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina titulo="Usuarios" subtitulo="Cuentas y roles del sistema">
        <button mat-flat-button type="button" (click)="abrirFormulario(null)">
          <mat-icon>add</mat-icon> Nuevo usuario
        </button>
      </app-cabecera-pagina>

      <mat-form-field appearance="outline" class="buscador">
        <mat-label>Buscar por usuario, nombre o correo</mat-label>
        <input matInput [value]="filtro()" (input)="filtro.set($any($event.target).value)" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="tabla">
        <table mat-table [dataSource]="visibles()">
          <ng-container matColumnDef="username">
            <th mat-header-cell *matHeaderCellDef>Usuario</th>
            <td mat-cell *matCellDef="let u">{{ u.username }}</td>
          </ng-container>
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let u">{{ u.nombres }} {{ u.apellidos }}</td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Correo</th>
            <td mat-cell *matCellDef="let u">{{ u.email }}</td>
          </ng-container>
          <ng-container matColumnDef="rol">
            <th mat-header-cell *matHeaderCellDef>Rol</th>
            <td mat-cell *matCellDef="let u">{{ u.rol }}</td>
          </ng-container>
          <ng-container matColumnDef="activo">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let u">
              <span class="estado" [class.inactivo]="!u.activo">{{ u.activo ? 'Activo' : 'Inactivo' }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let u" class="acciones">
              <button mat-icon-button type="button" matTooltip="Editar" aria-label="Editar" (click)="abrirFormulario(u)">
                <mat-icon>edit</mat-icon>
              </button>
              <button
                mat-icon-button
                type="button"
                [matTooltip]="esYo(u) ? 'No puedes eliminarte a ti mismo' : 'Eliminar'"
                aria-label="Eliminar"
                [disabled]="esYo(u)"
                (click)="eliminar(u)"
              >
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">{{ cargando() ? 'Cargando…' : 'No hay usuarios para mostrar' }}</td>
          </tr>
        </table>
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1100px; margin: 0 auto; }
    .buscador { width: 100%; max-width: 380px; }
    .tabla { overflow-x: auto; }
    table { width: 100%; }
    .acciones { text-align: right; white-space: nowrap; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
    .estado { font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #e6f4ea; color: #1e6b34; }
    .estado.inactivo { background: #eee; color: #666; }
  `,
})
export class Usuarios {
  private readonly api = inject(UsuariosApi);
  private readonly dialogo = inject(MatDialog);
  private readonly confirmar = inject(Confirmador);
  private readonly notificador = inject(Notificador);
  private readonly sesion = inject(SesionService);

  protected readonly columnas = ['username', 'nombre', 'email', 'rol', 'activo', 'acciones'];
  protected readonly filas = signal<Usuario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly filtro = signal('');
  protected readonly visibles = computed(() => {
    const t = this.filtro().trim().toLowerCase();
    return t
      ? this.filas().filter((u) =>
          [u.username, u.nombres, u.apellidos, u.email].some((campo) => campo.toLowerCase().includes(t)),
        )
      : this.filas();
  });

  constructor() {
    this.cargar();
  }

  protected esYo(usuario: Usuario): boolean {
    return usuario.username === this.sesion.usuario();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api
      .listar()
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (usuarios) => this.filas.set(usuarios),
        error: (error: unknown) => this.notificador.error(error, 'No se pudieron cargar los usuarios'),
      });
  }

  protected abrirFormulario(usuario: Usuario | null): void {
    this.dialogo
      .open(UsuarioForm, { data: usuario, width: '520px', maxWidth: '95vw', disableClose: true })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  protected eliminar(usuario: Usuario): void {
    this.confirmar
      .preguntar({
        titulo: 'Eliminar usuario',
        mensaje: `¿Eliminar a "${usuario.username}"? Esta acción no se puede deshacer. Si solo quieres que no entre, desactívalo al editarlo.`,
        aceptar: 'Eliminar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.eliminar(usuario.id)),
      )
      .subscribe({
        next: () => {
          this.notificador.ok('Usuario eliminado');
          this.cargar();
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo eliminar el usuario'),
      });
  }
}
