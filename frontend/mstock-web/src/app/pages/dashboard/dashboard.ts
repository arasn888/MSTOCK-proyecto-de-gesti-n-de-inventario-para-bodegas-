import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';
import { ReportesApi } from '../../core/api/reportes.api';
import { Dashboard } from '../../core/models/ventas';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Resumen de ventas por rango de fechas (solo ADMIN). Por defecto, hoy. */
@Component({
  selector: 'app-dashboard',
  imports: [CabeceraPagina, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressBarModule, MatSelectModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina titulo="Dashboard" subtitulo="Resumen de ventas y productos más vendidos" />

      <form class="filtros" (submit)="$event.preventDefault(); cargar()">
        <mat-form-field appearance="outline">
          <mat-label>Desde</mat-label>
          <input matInput type="date" [value]="desde()" [max]="hasta()" (input)="desde.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Hasta</mat-label>
          <input matInput type="date" [value]="hasta()" [min]="desde()" (input)="hasta.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="top">
          <mat-label>Top productos</mat-label>
          <mat-select [value]="top()" (selectionChange)="top.set($event.value)">
            @for (n of opcionesTop; track n) {
              <mat-option [value]="n">{{ n }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <button mat-flat-button type="submit" [disabled]="cargando() || !desde() || !hasta()">Consultar</button>
        <button mat-button type="button" (click)="rapido(0)" [disabled]="cargando()">Hoy</button>
        <button mat-button type="button" (click)="rapido(6)" [disabled]="cargando()">7 días</button>
        <button mat-button type="button" (click)="rapido(29)" [disabled]="cargando()">30 días</button>
      </form>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      @if (datos(); as d) {
        <div class="tarjetas">
          <div class="tarjeta">
            <span>Total vendido</span>
            <strong>S/ {{ d.ventas.total.toFixed(2) }}</strong>
            <small>{{ d.ventas.cantidad }} venta(s)</small>
          </div>
          <div class="tarjeta">
            <span>Ticket promedio</span>
            <strong>S/ {{ d.ventas.ticketPromedio.toFixed(2) }}</strong>
            <small>por venta</small>
          </div>
          <div class="tarjeta ganancia">
            <span>Ganancia</span>
            <strong>S/ {{ d.ventas.ganancia.toFixed(2) }}</strong>
            <small>venta menos costo</small>
          </div>
          <div class="tarjeta anulado">
            <span>Anulado</span>
            <strong>S/ {{ d.anuladas.total.toFixed(2) }}</strong>
            <small>{{ d.anuladas.cantidad }} venta(s)</small>
          </div>
        </div>

        @if (d.ventas.lineasSinCosto > 0) {
          <p class="aviso" role="status">
            {{ d.ventas.lineasSinCosto }} línea(s) de ventas antiguas no tienen costo registrado: la ganancia real puede ser mayor.
          </p>
        }

        <h2>Productos más vendidos</h2>
        @if (d.masVendidos.length === 0) {
          <p class="vacio">No hay ventas en este rango</p>
        } @else {
          <ol class="ranking">
            @for (p of d.masVendidos; track p.productoId) {
              <li>
                <div class="fila">
                  <span class="nombre">{{ p.nombre }} <small>{{ p.codigo }}</small></span>
                  <span class="cifras">{{ p.unidadesVendidas }} u · S/ {{ p.ingresos.toFixed(2) }}</span>
                </div>
                <div class="barra" role="img" [attr.aria-label]="p.nombre + ': ' + p.unidadesVendidas + ' unidades'">
                  <div class="relleno" [style.width.%]="ancho(p.unidadesVendidas)"></div>
                </div>
              </li>
            }
          </ol>
        }
      }
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1100px; margin: 0 auto; }
    .filtros { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .top { width: 130px; }
    .tarjetas { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin: 12px 0 16px; }
    .tarjeta { display: flex; flex-direction: column; gap: 2px; padding: 16px 20px; border-radius: 16px; background: #e8eefc; color: #1b3170; }
    .tarjeta.ganancia { background: #e6f4ea; color: #1e4d2b; }
    .tarjeta.anulado { background: #fdecea; color: #7a1f16; }
    .tarjeta strong { font-size: 26px; font-weight: 500; }
    .tarjeta small { opacity: 0.8; }
    .aviso { padding: 8px 12px; border-radius: 8px; background: #fff4e0; color: #8a5300; }
    h2 { font-size: 18px; font-weight: 500; margin: 20px 0 8px; }
    .ranking { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
    .fila { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
    .nombre small { opacity: 0.6; margin-left: 6px; }
    .cifras { opacity: 0.85; white-space: nowrap; }
    .barra { height: 10px; border-radius: 6px; background: color-mix(in srgb, currentColor 12%, transparent); overflow: hidden; }
    .relleno { height: 100%; border-radius: 6px; background: #3b5bdb; }
    .vacio { opacity: 0.7; }
  `,
})
export class DashboardPagina {
  private readonly api = inject(ReportesApi);
  private readonly notificador = inject(Notificador);

  protected readonly opcionesTop = [5, 10, 20];
  protected readonly desde = signal(iso(new Date()));
  protected readonly hasta = signal(iso(new Date()));
  protected readonly top = signal(5);
  protected readonly datos = signal<Dashboard | null>(null);
  protected readonly cargando = signal(false);

  private readonly maximo = computed(() => Math.max(1, ...(this.datos()?.masVendidos.map((p) => p.unidadesVendidas) ?? [1])));

  constructor() {
    this.cargar();
  }

  protected ancho(unidades: number): number {
    return Math.max(2, Math.round((unidades / this.maximo()) * 100));
  }

  /** Atajo: termina hoy y empieza `diasAtras` días antes. */
  protected rapido(diasAtras: number): void {
    const inicio = new Date();
    inicio.setDate(inicio.getDate() - diasAtras);
    this.desde.set(iso(inicio));
    this.hasta.set(iso(new Date()));
    this.cargar();
  }

  protected cargar(): void {
    if (!this.desde() || !this.hasta()) {
      return;
    }
    this.cargando.set(true);
    this.api
      .dashboard(this.desde(), this.hasta(), this.top())
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (d) => this.datos.set(d),
        error: (error: unknown) => this.notificador.error(error, 'No se pudo cargar el dashboard'),
      });
  }
}
