import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  filter,
  forkJoin,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { CategoriasApi } from '../../core/api/categorias.api';
import { ProductosApi } from '../../core/api/productos.api';
import { ProveedoresApi } from '../../core/api/proveedores.api';
import { SesionService } from '../../core/auth/sesion.service';
import { Categoria, Producto, Proveedor } from '../../core/models/catalogo';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { Confirmador } from '../../shared/confirmar';
import { ProductoForm } from './producto-form';

@Component({
  selector: 'app-productos',
  imports: [
    ReactiveFormsModule,
    CabeceraPagina,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina titulo="Productos" subtitulo="Catálogo de productos activos">
        @if (esAdmin()) {
          <button mat-flat-button type="button" (click)="abrirFormulario(null)">
            <mat-icon>add</mat-icon> Nuevo producto
          </button>
        }
      </app-cabecera-pagina>

      <div class="filtros">
        <mat-form-field appearance="outline" class="buscador">
          <mat-label>Buscar por nombre o código</mat-label>
          <input matInput [formControl]="nombre" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" class="categoria">
          <mat-label>Categoría</mat-label>
          <mat-select [formControl]="categoria">
            <mat-option [value]="null">Todas</mat-option>
            @for (c of categorias(); track c.id) {
              <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="tabla">
        <table mat-table [dataSource]="filas()">
          <ng-container matColumnDef="codigo">
            <th mat-header-cell *matHeaderCellDef>Código</th>
            <td mat-cell *matCellDef="let p">{{ p.codigo }}</td>
          </ng-container>
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let p">{{ p.nombre }}</td>
          </ng-container>
          <ng-container matColumnDef="categoria">
            <th mat-header-cell *matHeaderCellDef>Categoría</th>
            <td mat-cell *matCellDef="let p">{{ p.categoriaNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="proveedor">
            <th mat-header-cell *matHeaderCellDef>Proveedor</th>
            <td mat-cell *matCellDef="let p">{{ p.proveedorNombre ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="precioCompra">
            <th mat-header-cell *matHeaderCellDef class="num">Compra</th>
            <td mat-cell *matCellDef="let p" class="num">{{ p.precioCompra.toFixed(2) }}</td>
          </ng-container>
          <ng-container matColumnDef="precioVenta">
            <th mat-header-cell *matHeaderCellDef class="num">Venta</th>
            <td mat-cell *matCellDef="let p" class="num">{{ p.precioVenta.toFixed(2) }}</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let p" class="acciones">
              <button mat-icon-button type="button" matTooltip="Editar" aria-label="Editar"
                      (click)="abrirFormulario(p)"><mat-icon>edit</mat-icon></button>
              <button mat-icon-button type="button" matTooltip="Desactivar" aria-label="Desactivar"
                      (click)="desactivar(p)"><mat-icon>block</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas()"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas()"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas().length">
              {{ cargando() ? 'Cargando…' : 'No hay productos para mostrar' }}
            </td>
          </tr>
        </table>
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .filtros { display: flex; gap: 12px; flex-wrap: wrap; }
    .buscador { flex: 1 1 260px; max-width: 360px; }
    .categoria { flex: 1 1 200px; max-width: 260px; }
    .tabla { overflow-x: auto; }
    table { width: 100%; }
    .num { text-align: right; }
    .acciones { text-align: right; white-space: nowrap; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
  `,
})
export class Productos {
  private readonly api = inject(ProductosApi);
  private readonly categoriasApi = inject(CategoriasApi);
  private readonly proveedoresApi = inject(ProveedoresApi);
  private readonly dialogo = inject(MatDialog);
  private readonly confirmar = inject(Confirmador);
  private readonly notificador = inject(Notificador);
  private readonly sesion = inject(SesionService);

  /** Crear, editar y desactivar: solo ADMIN (el servidor lo exige también). */
  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly nombre = new FormControl('', { nonNullable: true });
  protected readonly categoria = new FormControl<number | null>(null);

  protected readonly filas = signal<Producto[]>([]);
  protected readonly categorias = signal<Categoria[]>([]);
  private readonly proveedores = signal<Proveedor[]>([]);
  protected readonly cargando = signal(true);
  protected readonly columnas = computed(() => {
    const base = ['codigo', 'nombre', 'categoria', 'proveedor', 'precioCompra', 'precioVenta'];
    return this.esAdmin() ? [...base, 'acciones'] : base;
  });

  private readonly recargar = new BehaviorSubject<void>(undefined);

  constructor() {
    // Categorías y proveedores: para el filtro y para el formulario.
    forkJoin({
      categorias: this.categoriasApi.listar(),
      proveedores: this.proveedoresApi.listar(),
    }).subscribe({
      next: ({ categorias, proveedores }) => {
        this.categorias.set(categorias);
        this.proveedores.set(proveedores);
      },
      error: (error: unknown) =>
        this.notificador.error(error, 'No se pudieron cargar las categorías y proveedores'),
    });

    combineLatest([
      this.nombre.valueChanges.pipe(startWith(this.nombre.value), debounceTime(300), distinctUntilChanged()),
      this.categoria.valueChanges.pipe(startWith(this.categoria.value)),
      this.recargar,
    ])
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(([nombre, categoriaId]) =>
          this.api.listar({ nombre, categoriaId: categoriaId ?? undefined }).pipe(
            catchError((error: unknown) => {
              this.notificador.error(error, 'No se pudieron cargar los productos');
              return of<Producto[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((productos) => {
        this.filas.set(productos);
        this.cargando.set(false);
      });
  }

  protected abrirFormulario(producto: Producto | null): void {
    this.dialogo
      .open(ProductoForm, {
        data: { producto, categorias: this.categorias(), proveedores: this.proveedores() },
        width: '640px',
        maxWidth: '95vw',
        disableClose: true,
      })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.recargar.next();
        }
      });
  }

  /** El servidor no borra el producto: lo marca inactivo (sigue en el historial de ventas y compras). */
  protected desactivar(producto: Producto): void {
    this.confirmar
      .preguntar({
        titulo: 'Desactivar producto',
        mensaje: `¿Desactivar "${producto.nombre}"? Dejará de aparecer en el catálogo y no se podrá vender ni comprar. Su historial se conserva.`,
        aceptar: 'Desactivar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.eliminar(producto.id)),
      )
      .subscribe({
        next: () => {
          this.notificador.ok('Producto desactivado');
          this.recargar.next();
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo desactivar el producto'),
      });
  }
}
