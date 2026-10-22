import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, finalize } from 'rxjs';
import { NotificacionesApi } from '../../core/api/notificaciones.api';
import { ContadorNotificaciones } from '../../core/contador-notificaciones';
import { Notificacion, TipoNotificacion } from '../../core/models/notificaciones';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';

const ICONOS: Record<TipoNotificacion, string> = {
  STOCK_BAJO: 'warning',
  VENTA_REGISTRADA: 'point_of_sale',
  VENTA_ANULADA: 'cancel',
};

@Component({
  selector: 'app-notificaciones',
  imports: [
    DatePipe,
    CabeceraPagina,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina titulo="Notificaciones" subtitulo="Alertas de stock bajo y de ventas">
        <button mat-stroked-button type="button" [disabled]="sinLeer() === 0 || trabajando()"
                (click)="marcarTodas()">
          <mat-icon>done_all</mat-icon> Marcar todas como leídas
        </button>
      </app-cabecera-pagina>

      <mat-slide-toggle [checked]="soloNoLeidas()" (change)="cambiarFiltro($event.checked)">
        Solo no leídas
      </mat-slide-toggle>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <div class="lista">
        @for (n of lista(); track n.id) {
          <mat-card appearance="outlined" [class.nueva]="!n.leida">
            <mat-card-content class="fila">
              <mat-icon class="icono" [class]="'icono ' + n.tipo.toLowerCase()">{{ icono(n.tipo) }}</mat-icon>
              <div class="texto">
                <div class="titulo">
                  {{ n.titulo }}
                  @if (!n.leida) {
                    <span class="etiqueta">Nueva</span>
                  }
                </div>
                <div class="mensaje">{{ n.mensaje }}</div>
                <div class="fecha">{{ n.fecha | date: 'dd/MM/yyyy HH:mm' }}</div>
              </div>
              <div class="acciones">
                @if (!n.leida) {
                  <button mat-icon-button type="button" matTooltip="Marcar como leída" aria-label="Marcar como leída"
                          [disabled]="trabajando()" (click)="marcarLeida(n)"><mat-icon>check</mat-icon></button>
                }
                <button mat-icon-button type="button" matTooltip="Eliminar" aria-label="Eliminar"
                        [disabled]="trabajando()" (click)="eliminar(n)"><mat-icon>delete</mat-icon></button>
              </div>
            </mat-card-content>
          </mat-card>
        } @empty {
          @if (!cargando()) {
            <p class="vacio">
              {{ soloNoLeidas() ? 'No tienes notificaciones sin leer' : 'Todavía no hay notificaciones' }}
            </p>
          }
        }
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 900px; margin: 0 auto; }
    .lista { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; }
    .fila { display: flex; gap: 12px; align-items: flex-start; }
    .texto { flex: 1 1 auto; min-width: 0; }
    .titulo { font-weight: 500; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .mensaje { margin-top: 2px; overflow-wrap: anywhere; }
    .fecha { margin-top: 4px; font-size: 12px; opacity: 0.65; }
    .acciones { display: flex; flex: 0 0 auto; }
    .nueva { border-left: 4px solid var(--mat-sys-primary, #3f51b5); }
    .etiqueta { font-size: 11px; padding: 1px 8px; border-radius: 10px; background: var(--mat-sys-primary, #3f51b5); color: #fff; }
    .icono { margin-top: 4px; }
    .icono.stock_bajo { color: #e08a00; }
    .icono.venta_anulada { color: #c62828; }
    .icono.venta_registrada { color: #2e7d32; }
    .vacio { text-align: center; opacity: 0.7; padding: 32px 0; }
  `,
})
export class Notificaciones {
  private readonly api = inject(NotificacionesApi);
  private readonly contador = inject(ContadorNotificaciones);
  private readonly notificador = inject(Notificador);

  protected readonly lista = signal<Notificacion[]>([]);
  protected readonly cargando = signal(true);
  protected readonly trabajando = signal(false);
  protected readonly soloNoLeidas = signal(false);
  protected readonly sinLeer = computed(() => this.lista().filter((n) => !n.leida).length);

  constructor() {
    this.cargar();
  }

  protected icono(tipo: TipoNotificacion): string {
    return ICONOS[tipo] ?? 'notifications';
  }

  protected cambiarFiltro(soloNoLeidas: boolean): void {
    this.soloNoLeidas.set(soloNoLeidas);
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api
      .listar(this.soloNoLeidas())
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (lista) => this.lista.set(lista),
        error: (error: unknown) => this.notificador.error(error, 'No se pudieron cargar las notificaciones'),
      });
    this.contador.refrescar();
  }

  protected marcarLeida(notificacion: Notificacion): void {
    this.ejecutar(this.api.marcarLeida(notificacion.id), 'No se pudo marcar como leída');
  }

  protected marcarTodas(): void {
    this.ejecutar(this.api.marcarTodasLeidas(), 'No se pudieron marcar como leídas');
  }

  protected eliminar(notificacion: Notificacion): void {
    this.ejecutar(this.api.eliminar(notificacion.id), 'No se pudo eliminar la notificación');
  }

  private ejecutar(peticion: Observable<unknown>, errorPorDefecto: string): void {
    this.trabajando.set(true);
    peticion.pipe(finalize(() => this.trabajando.set(false))).subscribe({
      next: () => this.cargar(),
      error: (error: unknown) => this.notificador.error(error, errorPorDefecto),
    });
  }
}
