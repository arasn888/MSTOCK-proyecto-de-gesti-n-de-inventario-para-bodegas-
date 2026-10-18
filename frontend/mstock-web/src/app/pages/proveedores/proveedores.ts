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
import { ProveedoresApi } from '../../core/api/proveedores.api';
import { SesionService } from '../../core/auth/sesion.service';
import { Proveedor } from '../../core/models/catalogo';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { Confirmador } from '../../shared/confirmar';
import { ProveedorForm } from './proveedor-form';

@Component({
  selector: 'app-proveedores',
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
      <app-cabecera-pagina titulo="Proveedores" subtitulo="Quienes abastecen la mercadería">
        @if (esAdmin()) {
          <button mat-flat-button type="button" (click)="abrirFormulario(null)">
            <mat-icon>add</mat-icon> Nuevo proveedor
          </button>
        }
      </app-cabecera-pagina>

      <mat-form-field appearance="outline" class="buscador">
        <mat-label>Buscar por razón social o RUC</mat-label>
        <input matInput [value]="filtro()" (input)="filtro.set($any($event.target).value)" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="tabla">
        <table mat-table [dataSource]="visibles()">
          <ng-container matColumnDef="ruc">
            <th mat-header-cell *matHeaderCellDef>RUC</th>
            <td mat-cell *matCellDef="let c">{{ c.ruc }}</td>
          </ng-container>
          <ng-container matColumnDef="razonSocial">
            <th mat-header-cell *matHeaderCellDef>Razón social</th>
            <td mat-cell *matCellDef="let c">{{ c.razonSocial }}</td>
          </ng-container>
          <ng-container matColumnDef="telefono">
            <th mat-header-cell *matHeaderCellDef>Teléfono</th>
            <td mat-cell *matCellDef="let c">{{ c.telefono ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Correo</th>
            <td mat-cell *matCellDef="let c">{{ c.email ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="activo">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let c">
              <span class="estado" [class.inactivo]="!c.activo">{{ c.activo ? 'Activo' : 'Inactivo' }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let c" class="acciones">
              <button mat-icon-button type="button" matTooltip="Editar" aria-label="Editar"
                      (click)="abrirFormulario(c)"><mat-icon>edit</mat-icon></button>
              <button mat-icon-button type="button" matTooltip="Eliminar" aria-label="Eliminar"
                      (click)="eliminar(c)"><mat-icon>delete</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas()"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas()"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas().length">
              {{ cargando() ? 'Cargando…' : 'No hay proveedores para mostrar' }}
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
    .estado { font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #e6f4ea; color: #1e6b34; }
    .estado.inactivo { background: #eee; color: #666; }
  `,
})
export class Proveedores {
  private readonly api = inject(ProveedoresApi);
  private readonly dialogo = inject(MatDialog);
  private readonly confirmar = inject(Confirmador);
  private readonly notificador = inject(Notificador);
  private readonly sesion = inject(SesionService);

  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly filas = signal<Proveedor[]>([]);
  protected readonly cargando = signal(true);
  protected readonly filtro = signal('');
  protected readonly visibles = computed(() => {
    const texto = this.filtro().trim().toLowerCase();
    return texto ? this.filas().filter((c) => c.razonSocial.toLowerCase().includes(texto) || c.ruc.includes(texto)) : this.filas();
  });
  protected readonly columnas = computed(() =>
    this.esAdmin() ? ['ruc', 'razonSocial', 'telefono', 'email', 'activo', 'acciones'] : ['ruc', 'razonSocial', 'telefono', 'email', 'activo'],
  );

  constructor() {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api
      .listar()
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (proveedores) => this.filas.set(proveedores),
        error: (error: unknown) => this.notificador.error(error, 'No se pudieron cargar los proveedores'),
      });
  }

  protected abrirFormulario(proveedor: Proveedor | null): void {
    this.dialogo
      .open(ProveedorForm, { data: proveedor, width: '520px', disableClose: true })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  protected eliminar(proveedor: Proveedor): void {
    this.confirmar
      .preguntar({
        titulo: 'Eliminar proveedor',
        mensaje: `¿Eliminar al proveedor "${proveedor.razonSocial}"? Solo se puede si no tiene productos.`,
        aceptar: 'Eliminar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.eliminar(proveedor.id)),
      )
      .subscribe({
        next: () => {
          this.notificador.ok('Proveedor eliminado');
          this.cargar();
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo eliminar el proveedor'),
      });
  }
}
