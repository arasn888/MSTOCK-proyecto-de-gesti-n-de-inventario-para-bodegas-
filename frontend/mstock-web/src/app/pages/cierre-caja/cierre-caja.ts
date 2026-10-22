import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { finalize } from 'rxjs';
import { ReportesApi } from '../../core/api/reportes.api';
import { SesionService } from '../../core/auth/sesion.service';
import { CierreCaja } from '../../core/models/ventas';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';

/** Fecha local yyyy-MM-dd (sin pasar por UTC). */
function hoyIso(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}

/** Cierre de caja del día: ADMIN ve todos los cajeros; un CAJERO solo el suyo (lo decide el servidor). */
@Component({
  selector: 'app-cierre-caja',
  imports: [
    CabeceraPagina,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina
        titulo="Cierre de caja"
        [subtitulo]="esAdmin() ? 'Ventas del día por cajero' : 'Tus ventas del día'"
      />

      <form class="filtros" (submit)="$event.preventDefault(); cargar()">
        <mat-form-field appearance="outline">
          <mat-label>Fecha</mat-label>
          <input matInput type="date" [value]="fecha()" [max]="maxima" (input)="fecha.set($any($event.target).value)" />
        </mat-form-field>
        <button mat-stroked-button type="submit" [disabled]="cargando() || !fecha()">Consultar</button>
        <button mat-button type="button" (click)="hoy()" [disabled]="cargando()">Hoy</button>
        <button mat-button type="button" (click)="imprimir()" [disabled]="!cierre()">
          <mat-icon>print</mat-icon> Imprimir
        </button>
      </form>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      @if (cierre(); as c) {
        <div class="tarjetas">
          <div class="tarjeta principal">
            <span>Total vendido (efectivo esperado)</span>
            <strong>S/ {{ c.totales.totalVendido.toFixed(2) }}</strong>
            <small>{{ c.totales.cantidadVentas }} venta(s)</small>
          </div>
          <div class="tarjeta">
            <span>Anuladas (no suman)</span>
            <strong>S/ {{ c.totales.totalAnulado.toFixed(2) }}</strong>
            <small>{{ c.totales.cantidadAnuladas }} venta(s)</small>
          </div>
        </div>

        <div class="tabla">
          <table mat-table [dataSource]="c.cajeros">
            <ng-container matColumnDef="usuario">
              <th mat-header-cell *matHeaderCellDef>Cajero</th>
              <td mat-cell *matCellDef="let f">{{ f.usuario }}</td>
              <td mat-footer-cell *matFooterCellDef><strong>Total</strong></td>
            </ng-container>
            <ng-container matColumnDef="cantidadVentas">
              <th mat-header-cell *matHeaderCellDef class="num">Ventas</th>
              <td mat-cell *matCellDef="let f" class="num">{{ f.cantidadVentas }}</td>
              <td mat-footer-cell *matFooterCellDef class="num"><strong>{{ c.totales.cantidadVentas }}</strong></td>
            </ng-container>
            <ng-container matColumnDef="totalVendido">
              <th mat-header-cell *matHeaderCellDef class="num">Vendido</th>
              <td mat-cell *matCellDef="let f" class="num">S/ {{ f.totalVendido.toFixed(2) }}</td>
              <td mat-footer-cell *matFooterCellDef class="num"><strong>S/ {{ c.totales.totalVendido.toFixed(2) }}</strong></td>
            </ng-container>
            <ng-container matColumnDef="cantidadAnuladas">
              <th mat-header-cell *matHeaderCellDef class="num">Anuladas</th>
              <td mat-cell *matCellDef="let f" class="num">{{ f.cantidadAnuladas }}</td>
              <td mat-footer-cell *matFooterCellDef class="num"><strong>{{ c.totales.cantidadAnuladas }}</strong></td>
            </ng-container>
            <ng-container matColumnDef="totalAnulado">
              <th mat-header-cell *matHeaderCellDef class="num">Monto anulado</th>
              <td mat-cell *matCellDef="let f" class="num">S/ {{ f.totalAnulado.toFixed(2) }}</td>
              <td mat-footer-cell *matFooterCellDef class="num"><strong>S/ {{ c.totales.totalAnulado.toFixed(2) }}</strong></td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columnas"></tr>
            <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
            <tr mat-footer-row *matFooterRowDef="columnas"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="vacio" [attr.colspan]="columnas.length">No hay ventas en esta fecha</td>
            </tr>
          </table>
        </div>
      }
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1000px; margin: 0 auto; }
    .filtros { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .tarjetas { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin: 8px 0 20px; }
    .tarjeta {
      display: flex; flex-direction: column; gap: 4px; padding: 16px; border-radius: 16px;
      border: 1px solid color-mix(in srgb, currentColor 22%, transparent);
    }
    .tarjeta strong { font-size: 28px; font-weight: 500; }
    .tarjeta.principal { background: color-mix(in srgb, #1e6b34 10%, transparent); }
    .tarjeta small { opacity: 0.7; }
    .tabla { overflow-x: auto; }
    table { width: 100%; }
    .num { text-align: right; }
    .vacio { padding: 24px; text-align: center; opacity: 0.7; }
    @media print { .filtros { display: none; } }
  `,
})
export class CierreCajaPagina {
  private readonly api = inject(ReportesApi);
  private readonly notificador = inject(Notificador);
  private readonly sesion = inject(SesionService);

  protected readonly columnas = ['usuario', 'cantidadVentas', 'totalVendido', 'cantidadAnuladas', 'totalAnulado'];
  protected readonly maxima = hoyIso();
  protected readonly esAdmin = computed(() => this.sesion.rol() === 'ADMIN');
  protected readonly fecha = signal(hoyIso());
  protected readonly cierre = signal<CierreCaja | null>(null);
  protected readonly cargando = signal(false);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    if (!this.fecha()) {
      return;
    }
    this.cargando.set(true);
    this.api
      .cierreCaja(this.fecha())
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (cierre) => this.cierre.set(cierre),
        error: (error: unknown) => this.notificador.error(error, 'No se pudo cargar el cierre de caja'),
      });
  }

  protected hoy(): void {
    this.fecha.set(hoyIso());
    this.cargar();
  }

  protected imprimir(): void {
    window.print();
  }
}
