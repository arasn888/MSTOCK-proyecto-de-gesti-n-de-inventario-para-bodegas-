import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize, forkJoin } from 'rxjs';
import { InventarioApi } from '../../core/api/inventario.api';
import { ProductosApi } from '../../core/api/productos.api';
import { Producto } from '../../core/models/catalogo';
import { Stock } from '../../core/models/inventario';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { KardexDialogo } from './kardex-dialogo';
import { DatosMovimiento, MovimientoForm, TipoOperacion } from './movimiento-form';

interface Fila {
  stock: Stock;
  codigo: string;
  nombre: string;
}

@Component({
  selector: 'app-inventario',
  imports: [
    CabeceraPagina,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina titulo="Inventario" subtitulo="Stock actual, alertas y movimientos">
        <button mat-flat-button type="button" (click)="abrirMovimiento('ENTRADA', null)">
          <mat-icon>add</mat-icon> Registrar entrada
        </button>
      </app-cabecera-pagina>

      <div class="filtros">
        <mat-form-field appearance="outline" class="buscador">
          <mat-label>Buscar por código o nombre</mat-label>
          <input matInput [value]="texto()" (input)="texto.set($any($event.target).value)" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-slide-toggle [checked]="soloCriticos()" (change)="soloCriticos.set($event.checked)">
          Solo stock crítico ({{ criticos() }})
        </mat-slide-toggle>
      </div>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="tabla">
        <table mat-table [dataSource]="filas()">
          <ng-container matColumnDef="codigo">
            <th mat-header-cell *matHeaderCellDef>Código</th>
            <td mat-cell *matCellDef="let f">{{ f.codigo }}</td>
          </ng-container>
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Producto</th>
            <td mat-cell *matCellDef="let f">{{ f.nombre }}</td>
          </ng-container>
          <ng-container matColumnDef="cantidad">
            <th mat-header-cell *matHeaderCellDef class="num">Stock</th>
            <td mat-cell *matCellDef="let f" class="num"><strong>{{ f.stock.cantidad }}</strong></td>
          </ng-container>
          <ng-container matColumnDef="minimo">
            <th mat-header-cell *matHeaderCellDef class="num">Mínimo</th>
            <td mat-cell *matCellDef="let f" class="num">{{ f.stock.stockMinimo }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let f">
              <span class="estado" [class.bajo]="f.stock.bajoMinimo">
                {{ f.stock.bajoMinimo ? 'Stock bajo' : 'Normal' }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let f" class="acciones">
              <button mat-icon-button type="button" matTooltip="Entrada" aria-label="Entrada"
                      (click)="abrirMovimiento('ENTRADA', f)"><mat-icon>add_circle_outline</mat-icon></button>
              <button mat-icon-button type="button" matTooltip="Salida" aria-label="Salida"
                      (click)="abrirMovimiento('SALIDA', f)"><mat-icon>remove_circle_outline</mat-icon></button>
              <button mat-icon-button type="button" matTooltip="Ajustar con conteo físico" aria-label="Ajustar"
                      (click)="abrirMovimiento('AJUSTE', f)"><mat-icon>tune</mat-icon></button>
              <button mat-icon-button type="button" matTooltip="Stock mínimo" aria-label="Stock mínimo"
                      (click)="abrirMovimiento('MINIMO', f)"><mat-icon>flag</mat-icon></button>
              <button mat-icon-button type="button" matTooltip="Kardex" aria-label="Kardex"
                      (click)="verKardex(f)"><mat-icon>history</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas" [class.fila-baja]="fila.stock.bajoMinimo"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">
              {{ cargando() ? 'Cargando…' : 'No hay productos con stock registrado' }}
            </td>
          </tr>
        </table>
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .filtros { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; margin-bottom: 8px; }
    .buscador { flex: 1 1 280px; max-width: 380px; }
    .tabla { overflow-x: auto; }
    table { width: 100%; }
    .num { text-align: right; }
    .acciones { text-align: right; white-space: nowrap; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
    .estado { font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #e6f4ea; color: #1e6b34; }
    .estado.bajo { background: #fdecea; color: #a1281c; }
    .fila-baja { background: color-mix(in srgb, #e53935 6%, transparent); }
  `,
})
export class Inventario {
  private readonly api = inject(InventarioApi);
  private readonly productosApi = inject(ProductosApi);
  private readonly dialogo = inject(MatDialog);
  private readonly notificador = inject(Notificador);

  protected readonly columnas = ['codigo', 'nombre', 'cantidad', 'minimo', 'estado', 'acciones'];
  private readonly productos = signal<Producto[]>([]);
  private readonly stock = signal<Stock[]>([]);
  protected readonly cargando = signal(true);
  protected readonly texto = signal('');
  protected readonly soloCriticos = signal(false);

  /** El stock solo trae el id del producto: nombre y código se toman del catálogo. */
  private readonly todas = computed<Fila[]>(() => {
    const porId = new Map(this.productos().map((p) => [p.id, p]));
    return this.stock().map((s) => {
      const producto = porId.get(s.productoId);
      return {
        stock: s,
        codigo: producto?.codigo ?? '—',
        nombre: producto?.nombre ?? `Producto #${s.productoId} (inactivo o eliminado)`,
      };
    });
  });
  protected readonly criticos = computed(() => this.todas().filter((f) => f.stock.bajoMinimo).length);
  protected readonly filas = computed(() => {
    const t = this.texto().trim().toLowerCase();
    return this.todas().filter(
      (f) =>
        (!this.soloCriticos() || f.stock.bajoMinimo) &&
        (t === '' || f.codigo.toLowerCase().includes(t) || f.nombre.toLowerCase().includes(t)),
    );
  });

  constructor() {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    forkJoin({ productos: this.productosApi.listar(), stock: this.api.listar() })
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: ({ productos, stock }) => {
          this.productos.set(productos);
          this.stock.set(stock);
        },
        error: (error: unknown) => this.notificador.error(error, 'No se pudo cargar el inventario'),
      });
  }

  protected abrirMovimiento(tipo: TipoOperacion, fila: Fila | null): void {
    const datos: DatosMovimiento = {
      tipo,
      productos: this.productos(),
      stock: fila?.stock ?? null,
      nombreProducto: fila ? `${fila.codigo} · ${fila.nombre}` : undefined,
    };
    this.dialogo
      .open(MovimientoForm, { data: datos, width: '520px', maxWidth: '95vw', disableClose: true })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  protected verKardex(fila: Fila): void {
    this.dialogo.open(KardexDialogo, {
      data: { productoId: fila.stock.productoId, nombre: fila.nombre },
      width: '900px',
      maxWidth: '95vw',
    });
  }
}
