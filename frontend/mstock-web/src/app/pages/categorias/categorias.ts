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
import { CategoriasApi } from '../../core/api/categorias.api';
import { SesionService } from '../../core/auth/sesion.service';
import { Categoria } from '../../core/models/catalogo';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { Confirmador } from '../../shared/confirmar';
import { CategoriaForm } from './categoria-form';

@Component({
  selector: 'app-categorias',
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
      <app-cabecera-pagina titulo="Categorías" subtitulo="Agrupan los productos del catálogo">
        @if (esAdmin()) {
          <button mat-flat-button type="button" (click)="abrirFormulario(null)">
            <mat-icon>add</mat-icon> Nueva categoría
          </button>
        }
      </app-cabecera-pagina>

      <mat-form-field appearance="outline" class="buscador">
        <mat-label>Buscar por nombre</mat-label>
        <input matInput [value]="filtro()" (input)="filtro.set($any($event.target).value)" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="tabla">
        <table mat-table [dataSource]="visibles()">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let c">{{ c.nombre }}</td>
          </ng-container>
          <ng-container matColumnDef="descripcion">
            <th mat-header-cell *matHeaderCellDef>Descripción</th>
            <td mat-cell *matCellDef="let c">{{ c.descripcion ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="activo">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let c">
              <span class="estado" [class.inactivo]="!c.activo">{{ c.activo ? 'Activa' : 'Inactiva' }}</span>
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
              {{ cargando() ? 'Cargando…' : 'No hay categorías para mostrar' }}
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
export class Categorias {
  private readonly api = inject(CategoriasApi);
  private readonly dialogo = inject(MatDialog);
  private readonly confirmar = inject(Confirmador);
  private readonly notificador = inject(Notificador);
  private readonly sesion = inject(SesionService);

  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly filas = signal<Categoria[]>([]);
  protected readonly cargando = signal(true);
  protected readonly filtro = signal('');
  protected readonly visibles = computed(() => {
    const texto = this.filtro().trim().toLowerCase();
    return texto ? this.filas().filter((c) => c.nombre.toLowerCase().includes(texto)) : this.filas();
  });
  protected readonly columnas = computed(() =>
    this.esAdmin() ? ['nombre', 'descripcion', 'activo', 'acciones'] : ['nombre', 'descripcion', 'activo'],
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
        next: (categorias) => this.filas.set(categorias),
        error: (error: unknown) => this.notificador.error(error, 'No se pudieron cargar las categorías'),
      });
  }

  protected abrirFormulario(categoria: Categoria | null): void {
    this.dialogo
      .open(CategoriaForm, { data: categoria, width: '520px', disableClose: true })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  protected eliminar(categoria: Categoria): void {
    this.confirmar
      .preguntar({
        titulo: 'Eliminar categoría',
        mensaje: `¿Eliminar la categoría "${categoria.nombre}"? Solo se puede si no tiene productos.`,
        aceptar: 'Eliminar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.eliminar(categoria.id)),
      )
      .subscribe({
        next: () => {
          this.notificador.ok('Categoría eliminada');
          this.cargar();
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo eliminar la categoría'),
      });
  }
}
