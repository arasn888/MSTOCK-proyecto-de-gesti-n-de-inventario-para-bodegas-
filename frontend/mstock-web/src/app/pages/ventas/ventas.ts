import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { VentasApi } from '../../core/api/ventas.api';
import { abrirPdf } from '../../core/http/descargas';
import { VentaResumen } from '../../core/models/ventas';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { DatosVentaDetalle, VentaDetalle } from './venta-detalle';

/** Fecha local en formato yyyy-MM-dd (sin pasar por UTC, que en Perú cambiaría el día por la noche). */
function hoyIso(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}

/** Listado de ventas por rango de fechas (por defecto, hoy). */
@Component({
  selector: 'app-ventas',
  imports: [
    DatePipe,
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
      <app-cabecera-pagina titulo="Ventas" subtitulo="Historial, boletas y anulaciones">
        <button mat-flat-button type="button" (click)="irAlPuntoDeVenta()">
          <mat-icon>point_of_sale</mat-icon> Nueva venta
        </button>
      </app-cabecera-pagina>

      <form class="filtros" (submit)="$event.preventDefault(); cargar()">
        <mat-form-field appearance="outline">
          <mat-label>Desde</mat-label>
          <input matInput type="date" [value]="desde()" [max]="hasta()" (input)="desde.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Hasta</mat-label>
          <input matInput type="date" [value]="hasta()" [min]="desde()" (input)="hasta.set($any($event.target).value)" />
        </mat-form-field>
        <button mat-stroked-button type="submit" [disabled]="cargando() || !desde() || !hasta()">Buscar</button>
        <button mat-button type="button" (click)="hoy()" [disabled]="cargando()">Hoy</button>
      </form>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="resumen">
        <span>{{ completadas().length }} venta(s) completada(s)</span>
        <strong>Total vendido: S/ {{ totalVendido().toFixed(2) }}</strong>
        @if (anuladas() > 0) {
          <span class="aviso">{{ anuladas() }} anulada(s) no incluidas</span>
        }
      </div>

      <div class="tabla">
        <table mat-table [dataSource]="ventas()">
          <ng-container matColumnDef="numero">
            <th mat-header-cell *matHeaderCellDef>N°</th>
            <td mat-cell *matCellDef="let v">{{ v.numero }}</td>
          </ng-container>
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let v">{{ v.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="cliente">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let v">{{ v.clienteNombre ?? 'Consumidor final' }}</td>
          </ng-container>
          <ng-container matColumnDef="usuario">
            <th mat-header-cell *matHeaderCellDef>Cajero</th>
            <td mat-cell *matCellDef="let v">{{ v.usuario }}</td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef class="num">Total</th>
            <td mat-cell *matCellDef="let v" class="num">S/ {{ v.total.toFixed(2) }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let v">
              <span class="estado" [class.anulada]="v.estado === 'ANULADA'">
                {{ v.estado === 'ANULADA' ? 'Anulada' : 'Completada' }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let v" class="acciones">
              <button mat-icon-button type="button" matTooltip="Ver detalle" aria-label="Ver detalle" (click)="verDetalle(v)">
                <mat-icon>visibility</mat-icon>
              </button>
              <button mat-icon-button type="button" matTooltip="Boleta PDF" aria-label="Boleta PDF" (click)="boleta(v)">
                <mat-icon>picture_as_pdf</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas" [class.fila-anulada]="fila.estado === 'ANULADA'"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="vacio" [attr.colspan]="columnas.length">
              {{ cargando() ? 'Cargando…' : 'No hay ventas en el rango elegido' }}
            </td>
          </tr>
        </table>
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .filtros { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .resumen { display: flex; gap: 20px; align-items: baseline; flex-wrap: wrap; margin: 8px 0 12px; }
    .aviso { opacity: 0.7; font-size: 13px; }
    .tabla { overflow-x: auto; }
    table { width: 100%; }
    .num { text-align: right; }
    .acciones { text-align: right; white-space: nowrap; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
    .estado { font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #e6f4ea; color: #1e6b34; }
    .estado.anulada { background: #fdecea; color: #a1281c; }
    .fila-anulada td { opacity: 0.6; text-decoration: line-through; }
    .fila-anulada td:last-child, .fila-anulada td:nth-last-child(2) { text-decoration: none; }
  `,
})
export class Ventas {
  private readonly api = inject(VentasApi);
  private readonly dialogo = inject(MatDialog);
  private readonly notificador = inject(Notificador);
  private readonly router = inject(Router);

  protected readonly columnas = ['numero', 'fecha', 'cliente', 'usuario', 'total', 'estado', 'acciones'];
  protected readonly desde = signal(hoyIso());
  protected readonly hasta = signal(hoyIso());
  protected readonly ventas = signal<VentaResumen[]>([]);
  protected readonly cargando = signal(false);

  protected readonly completadas = computed(() => this.ventas().filter((v) => v.estado === 'COMPLETADA'));
  protected readonly anuladas = computed(() => this.ventas().length - this.completadas().length);
  protected readonly totalVendido = computed(
    () => this.completadas().reduce((suma, v) => suma + Math.round(v.total * 100), 0) / 100,
  );

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    if (!this.desde() || !this.hasta()) {
      return;
    }
    this.cargando.set(true);
    this.api
      .listar({ desde: this.desde(), hasta: this.hasta() })
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (ventas) => this.ventas.set(ventas),
        error: (error: unknown) => this.notificador.error(error, 'No se pudieron cargar las ventas'),
      });
  }

  protected hoy(): void {
    this.desde.set(hoyIso());
    this.hasta.set(hoyIso());
    this.cargar();
  }

  protected verDetalle(venta: VentaResumen): void {
    const datos: DatosVentaDetalle = { ventaId: venta.id };
    this.dialogo
      .open(VentaDetalle, { data: datos, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((anulada) => {
        if (anulada) {
          this.cargar();
        }
      });
  }

  protected boleta(venta: VentaResumen): void {
    this.api.boleta(venta.id).subscribe({
      next: (pdf) => abrirPdf(pdf, `boleta-${venta.numero}.pdf`),
      error: (error: unknown) => this.notificador.error(error, 'No se pudo abrir la boleta'),
    });
  }

  protected irAlPuntoDeVenta(): void {
    void this.router.navigateByUrl('/ventas/nueva');
  }
}
