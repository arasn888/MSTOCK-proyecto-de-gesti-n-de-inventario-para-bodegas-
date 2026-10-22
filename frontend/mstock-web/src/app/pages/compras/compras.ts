import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize, forkJoin, timer } from 'rxjs';
import { ComprasApi } from '../../core/api/compras.api';
import { ProductosApi } from '../../core/api/productos.api';
import { ProveedoresApi } from '../../core/api/proveedores.api';
import { Producto, Proveedor } from '../../core/models/catalogo';
import { Compra } from '../../core/models/compras';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { CompraDetalle, DatosCompraDetalle } from './compra-detalle';
import { CompraForm, DatosCompraForm } from './compra-form';

const TEXTO_ESTADO = { PENDIENTE: 'Pendiente', RECIBIDA: 'Recibida', RECHAZADA: 'Rechazada' } as const;

/** Compras a proveedores. El ingreso al inventario es asíncrono: mientras haya pendientes, se actualiza sola. */
@Component({
  selector: 'app-compras',
  imports: [DatePipe, CabeceraPagina, MatTableModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina titulo="Compras" subtitulo="Mercadería que ingresa desde los proveedores">
        <button mat-stroked-button type="button" (click)="cargar()" [disabled]="cargando()">
          <mat-icon>refresh</mat-icon> Actualizar
        </button>
        <button mat-flat-button type="button" (click)="nueva()" [disabled]="!catalogoListo()">
          <mat-icon>add</mat-icon> Nueva compra
        </button>
      </app-cabecera-pagina>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      @if (pendientes() > 0) {
        <p class="aviso" role="status">
          {{ pendientes() }} compra(s) pendiente(s) de confirmación del inventario. Esta lista se actualiza sola.
        </p>
      }

      <div class="tabla">
        <table mat-table [dataSource]="compras()">
          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef>N°</th>
            <td mat-cell *matCellDef="let c">#{{ c.id }}</td>
          </ng-container>
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let c">{{ c.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="proveedor">
            <th mat-header-cell *matHeaderCellDef>Proveedor</th>
            <td mat-cell *matCellDef="let c">{{ nombreProveedor(c.proveedorId) }}</td>
          </ng-container>
          <ng-container matColumnDef="usuario">
            <th mat-header-cell *matHeaderCellDef>Registró</th>
            <td mat-cell *matCellDef="let c">{{ c.usuario }}</td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef class="num">Total</th>
            <td mat-cell *matCellDef="let c" class="num">S/ {{ c.total.toFixed(2) }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let c">
              <span class="estado" [class]="c.estado.toLowerCase()" [matTooltip]="c.motivoRechazo ?? ''">
                {{ texto(c.estado) }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let c" class="acciones">
              <button mat-icon-button type="button" matTooltip="Ver detalle" aria-label="Ver detalle" (click)="verDetalle(c)">
                <mat-icon>visibility</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">{{ cargando() ? 'Cargando…' : 'Aún no hay compras registradas' }}</td>
          </tr>
        </table>
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1100px; margin: 0 auto; }
    .aviso { padding: 8px 12px; border-radius: 8px; background: #fff4e0; color: #8a5300; }
    .tabla { overflow-x: auto; }
    table { width: 100%; }
    .num { text-align: right; }
    .acciones { text-align: right; white-space: nowrap; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
    .estado { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
    .estado.pendiente { background: #fff4e0; color: #8a5300; }
    .estado.recibida { background: #e6f4ea; color: #1e6b34; }
    .estado.rechazada { background: #fdecea; color: #a1281c; }
  `,
})
export class Compras {
  private readonly api = inject(ComprasApi);
  private readonly proveedoresApi = inject(ProveedoresApi);
  private readonly productosApi = inject(ProductosApi);
  private readonly dialogo = inject(MatDialog);
  private readonly notificador = inject(Notificador);

  protected readonly columnas = ['id', 'fecha', 'proveedor', 'usuario', 'total', 'estado', 'acciones'];
  protected readonly compras = signal<Compra[]>([]);
  protected readonly cargando = signal(true);
  private readonly proveedores = signal<Proveedor[]>([]);
  private readonly productos = signal<Producto[]>([]);
  protected readonly catalogoListo = computed(() => this.proveedores().length > 0 && this.productos().length > 0);
  protected readonly pendientes = computed(() => this.compras().filter((c) => c.estado === 'PENDIENTE').length);

  constructor() {
    this.cargar();
    forkJoin({
      proveedores: this.proveedoresApi.listar(),
      productos: this.productosApi.listar(),
    }).subscribe({
      next: ({ proveedores, productos }) => {
        this.proveedores.set(proveedores.filter((p) => p.activo));
        this.productos.set(productos);
      },
      error: (error: unknown) => this.notificador.error(error, 'No se pudo cargar proveedores y productos'),
    });

    // Cada 4 s, solo mientras haya compras pendientes de confirmación.
    timer(4000, 4000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (this.pendientes() > 0 && !this.cargando()) {
          this.cargar(true);
        }
      });
  }

  protected texto(estado: Compra['estado']): string {
    return TEXTO_ESTADO[estado];
  }

  protected nombreProveedor(id: number): string {
    return this.proveedores().find((p) => p.id === id)?.razonSocial ?? `Proveedor #${id}`;
  }

  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
    }
    this.api
      .listar()
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (compras) => this.compras.set([...compras].sort((a, b) => b.id - a.id)),
        error: (error: unknown) => {
          if (!silencioso) {
            this.notificador.error(error, 'No se pudieron cargar las compras');
          }
        },
      });
  }

  protected nueva(): void {
    const datos: DatosCompraForm = { proveedores: this.proveedores(), productos: this.productos() };
    this.dialogo
      .open(CompraForm, { data: datos, width: '760px', maxWidth: '95vw', disableClose: true })
      .afterClosed()
      .subscribe((compra: Compra | null | undefined) => {
        if (compra) {
          this.notificador.ok(`Compra #${compra.id} registrada; el inventario la confirmará en unos segundos`);
          this.cargar();
        }
      });
  }

  protected verDetalle(compra: Compra): void {
    const datos: DatosCompraDetalle = { compra, proveedor: this.nombreProveedor(compra.proveedorId) };
    this.dialogo.open(CompraDetalle, { data: datos, width: '640px', maxWidth: '95vw' });
  }
}
