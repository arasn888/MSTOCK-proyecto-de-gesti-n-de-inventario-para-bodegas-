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
import { ClientesApi } from '../../core/api/clientes.api';
import { SesionService } from '../../core/auth/sesion.service';
import { Cliente } from '../../core/models/ventas';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { Confirmador } from '../../shared/confirmar';
import { ClienteForm } from './cliente-form';

/** Clientes activos. Crear y editar: ADMIN y CAJERO. Eliminar (baja lógica): solo ADMIN. */
@Component({
  selector: 'app-clientes',
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
      <app-cabecera-pagina titulo="Clientes" subtitulo="Para asociarlos a las ventas (opcional)">
        <button mat-flat-button type="button" (click)="abrirFormulario(null)">
          <mat-icon>add</mat-icon> Nuevo cliente
        </button>
      </app-cabecera-pagina>

      <mat-form-field appearance="outline" class="buscador">
        <mat-label>Buscar por nombre o documento</mat-label>
        <input matInput [value]="filtro()" (input)="filtro.set($any($event.target).value)" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="tabla">
        <table mat-table [dataSource]="visibles()">
          <ng-container matColumnDef="documento">
            <th mat-header-cell *matHeaderCellDef>Documento</th>
            <td mat-cell *matCellDef="let c">{{ c.documento ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="nombres">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let c">{{ c.nombres }}</td>
          </ng-container>
          <ng-container matColumnDef="telefono">
            <th mat-header-cell *matHeaderCellDef>Teléfono</th>
            <td mat-cell *matCellDef="let c">{{ c.telefono ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Correo</th>
            <td mat-cell *matCellDef="let c">{{ c.email ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let c" class="acciones">
              <button mat-icon-button type="button" matTooltip="Editar" aria-label="Editar"
                      (click)="abrirFormulario(c)"><mat-icon>edit</mat-icon></button>
              @if (esAdmin()) {
                <button mat-icon-button type="button" matTooltip="Eliminar" aria-label="Eliminar"
                        (click)="eliminar(c)"><mat-icon>delete</mat-icon></button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">
              {{ cargando() ? 'Cargando…' : 'No hay clientes para mostrar' }}
            </td>
          </tr>
        </table>
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1100px; margin: 0 auto; }
    .buscador { width: 100%; max-width: 360px; }
    .tabla { overflow-x: auto; }
    table { width: 100%; }
    .acciones { text-align: right; white-space: nowrap; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
  `,
})
export class Clientes {
  private readonly api = inject(ClientesApi);
  private readonly dialogo = inject(MatDialog);
  private readonly confirmar = inject(Confirmador);
  private readonly notificador = inject(Notificador);
  private readonly sesion = inject(SesionService);

  protected readonly columnas = ['documento', 'nombres', 'telefono', 'email', 'acciones'];
  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly filas = signal<Cliente[]>([]);
  protected readonly cargando = signal(true);
  protected readonly filtro = signal('');
  protected readonly visibles = computed(() => {
    const texto = this.filtro().trim().toLowerCase();
    return texto
      ? this.filas().filter((c) => c.nombres.toLowerCase().includes(texto) || (c.documento ?? '').includes(texto))
      : this.filas();
  });

  constructor() {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api
      .listar()
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (clientes) => this.filas.set(clientes),
        error: (error: unknown) => this.notificador.error(error, 'No se pudieron cargar los clientes'),
      });
  }

  protected abrirFormulario(cliente: Cliente | null): void {
    this.dialogo
      .open(ClienteForm, { data: cliente, width: '520px', disableClose: true })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  protected eliminar(cliente: Cliente): void {
    this.confirmar
      .preguntar({
        titulo: 'Eliminar cliente',
        mensaje: `¿Eliminar a "${cliente.nombres}"? Sus ventas anteriores se conservan.`,
        aceptar: 'Eliminar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.eliminar(cliente.id)),
      )
      .subscribe({
        next: () => {
          this.notificador.ok('Cliente eliminado');
          this.cargar();
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo eliminar el cliente'),
      });
  }
}
