import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, catchError, debounceTime, finalize, of, switchMap, tap } from 'rxjs';
import { ClientesApi } from '../../core/api/clientes.api';
import { InventarioApi } from '../../core/api/inventario.api';
import { ProductosApi } from '../../core/api/productos.api';
import { VentasApi } from '../../core/api/ventas.api';
import { abrirPdf } from '../../core/http/descargas';
import { Producto } from '../../core/models/catalogo';
import { Cliente, Venta } from '../../core/models/ventas';
import { Notificador } from '../../core/notificador.service';
import { CabeceraPagina } from '../../shared/cabecera-pagina';
import { Confirmador } from '../../shared/confirmar';

interface Linea {
  producto: Producto;
  cantidad: number;
}

/** Máximo de productos que se muestran en la lista (el resto se alcanza escribiendo en el buscador). */
const MAX_RESULTADOS = 40;
/** Igual que el backend: hasta 10 000 unidades por producto. */
const MAX_CANTIDAD = 10_000;

const aCentimos = (importe: number): number => Math.round(importe * 100);

/**
 * Punto de venta: busca productos por nombre o código (también con lector de código de barras: escribe el
 * código y Enter), arma el carrito y cobra. El precio lo pone siempre el servidor; lo que se ve aquí es
 * solo una vista previa. El stock mostrado sirve de ayuda: la validación real la hace ventas-service.
 */
@Component({
  selector: 'app-punto-venta',
  imports: [
    CabeceraPagina,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <app-cabecera-pagina titulo="Punto de venta" subtitulo="Busca o escanea los productos y cobra" />

      @if (ultimaVenta(); as venta) {
        <div class="exito" role="status">
          <mat-icon>check_circle</mat-icon>
          <div class="exito-texto">
            <strong>Venta {{ venta.numero }} registrada</strong>
            <span>Total S/ {{ venta.total.toFixed(2) }}</span>
          </div>
          <button mat-stroked-button type="button" (click)="verBoleta(venta)" [disabled]="abriendoBoleta()">
            <mat-icon>picture_as_pdf</mat-icon> Ver boleta
          </button>
          <button mat-icon-button type="button" aria-label="Cerrar aviso" (click)="ultimaVenta.set(null)">
            <mat-icon>close</mat-icon>
          </button>
        </div>
      }

      <div class="disposicion">
        <div class="catalogo">
          <mat-form-field appearance="outline" class="buscador">
            <mat-label>Buscar por nombre o código (Enter para agregar)</mat-label>
            <input
              #buscador
              matInput
              autocomplete="off"
              [value]="texto()"
              (input)="escribir($any($event.target).value)"
              (keydown.enter)="enter($event)"
            />
            <mat-icon matSuffix>qr_code_scanner</mat-icon>
          </mat-form-field>

          @if (buscando()) {
            <mat-progress-bar mode="indeterminate" />
          }

          <div class="lista">
            @for (p of resultados(); track p.id) {
              @let disponible = stockDe(p.id);
              <button
                type="button"
                class="producto"
                [disabled]="disponible <= 0"
                (click)="agregar(p)"
                [attr.aria-label]="'Agregar ' + p.nombre"
              >
                <span class="nombre">{{ p.nombre }}</span>
                <span class="codigo">{{ p.codigo }} · {{ p.categoriaNombre }}</span>
                <span class="fila-precio">
                  <strong>S/ {{ p.precioVenta.toFixed(2) }}</strong>
                  <span class="stock" [class.sin]="disponible <= 0" [class.poco]="disponible > 0 && disponible <= 5">
                    {{ disponible <= 0 ? 'Sin stock' : 'Stock ' + disponible }}
                  </span>
                </span>
              </button>
            } @empty {
              <p class="vacio">
                {{ buscando() ? 'Buscando…' : texto().trim() ? 'No se encontraron productos' : 'No hay productos activos' }}
              </p>
            }
          </div>
          @if (hayMas()) {
            <p class="nota">Se muestran los primeros {{ maxResultados }}. Escribe para afinar la búsqueda.</p>
          }
        </div>

        <aside class="carrito" aria-label="Carrito de venta">
          <h2>Carrito ({{ unidades() }})</h2>

          <div class="lineas">
            @for (l of carrito(); track l.producto.id) {
              @let disponible = stockDe(l.producto.id);
              <div class="linea">
                <div class="linea-info">
                  <span class="nombre">{{ l.producto.nombre }}</span>
                  <span class="codigo">S/ {{ l.producto.precioVenta.toFixed(2) }} c/u</span>
                  @if (l.cantidad > disponible) {
                    <span class="alerta">Solo hay {{ disponible }} en stock</span>
                  }
                </div>
                <div class="cantidad">
                  <button mat-icon-button type="button" aria-label="Quitar una unidad" (click)="cambiar(l, l.cantidad - 1)">
                    <mat-icon>remove</mat-icon>
                  </button>
                  <input
                    type="number"
                    min="1"
                    [max]="maximo(l)"
                    [value]="l.cantidad"
                    aria-label="Cantidad"
                    (change)="cambiar(l, $any($event.target).valueAsNumber, $event)"
                  />
                  <button mat-icon-button type="button" aria-label="Agregar una unidad" (click)="cambiar(l, l.cantidad + 1)">
                    <mat-icon>add</mat-icon>
                  </button>
                </div>
                <strong class="subtotal">S/ {{ subtotal(l).toFixed(2) }}</strong>
                <button mat-icon-button type="button" matTooltip="Quitar" aria-label="Quitar del carrito" (click)="quitar(l)">
                  <mat-icon>delete_outline</mat-icon>
                </button>
              </div>
            } @empty {
              <p class="vacio">Aún no hay productos en el carrito</p>
            }
          </div>

          <mat-form-field appearance="outline" class="cliente">
            <mat-label>Cliente (opcional)</mat-label>
            <mat-select [value]="clienteId()" (selectionChange)="clienteId.set($event.value)">
              <mat-option [value]="null">Consumidor final</mat-option>
              @for (c of clientes(); track c.id) {
                <mat-option [value]="c.id">
                  {{ c.nombres }}{{ c.documento ? ' · ' + c.documento : '' }}
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <div class="total">
            <span>Total</span>
            <strong>S/ {{ total().toFixed(2) }}</strong>
          </div>

          <div class="botones">
            <button mat-button type="button" [disabled]="carrito().length === 0 || cobrando()" (click)="vaciar()">
              Vaciar
            </button>
            <button
              mat-flat-button
              type="button"
              class="cobrar"
              [disabled]="carrito().length === 0 || cobrando()"
              (click)="cobrar()"
            >
              <mat-icon>point_of_sale</mat-icon> {{ cobrando() ? 'Cobrando…' : 'Cobrar' }}
            </button>
          </div>
          @if (cobrando()) {
            <mat-progress-bar mode="indeterminate" />
          }
        </aside>
      </div>
    </section>
  `,
  styles: `
    .pagina { padding: 24px; max-width: 1300px; margin: 0 auto; }
    .disposicion { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr); gap: 24px; align-items: start; }
    @media (max-width: 900px) { .disposicion { grid-template-columns: 1fr; } }
    .buscador { width: 100%; }
    .lista { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 10px; }
    .producto {
      display: flex; flex-direction: column; gap: 4px; text-align: left; cursor: pointer; font: inherit;
      padding: 12px; border-radius: 12px; color: inherit; background: transparent;
      border: 1px solid color-mix(in srgb, currentColor 22%, transparent);
    }
    .producto:hover:not(:disabled) { background: color-mix(in srgb, currentColor 7%, transparent); }
    .producto:focus-visible { outline: 2px solid currentColor; outline-offset: 2px; }
    .producto:disabled { opacity: 0.5; cursor: not-allowed; }
    .nombre { font-weight: 500; overflow-wrap: anywhere; }
    .codigo { font-size: 12px; opacity: 0.7; }
    .fila-precio { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; gap: 8px; }
    .stock { font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #e6f4ea; color: #1e6b34; white-space: nowrap; }
    .stock.poco { background: #fff4e0; color: #8a5300; }
    .stock.sin { background: #fdecea; color: #a1281c; }
    .vacio, .nota { opacity: 0.7; padding: 16px 0; margin: 0; }
    .nota { font-size: 12px; padding-top: 8px; }
    .carrito {
      position: sticky; top: 16px; padding: 16px; border-radius: 16px;
      border: 1px solid color-mix(in srgb, currentColor 22%, transparent);
    }
    .carrito h2 { margin: 0 0 8px; font-size: 18px; font-weight: 500; }
    .lineas { max-height: 46vh; overflow-y: auto; margin-bottom: 12px; }
    .linea { display: grid; grid-template-columns: 1fr auto auto auto; gap: 8px; align-items: center; padding: 8px 0; border-bottom: 1px solid color-mix(in srgb, currentColor 12%, transparent); }
    .linea-info { display: flex; flex-direction: column; min-width: 0; }
    .alerta { font-size: 12px; color: #a1281c; }
    .cantidad { display: flex; align-items: center; }
    .cantidad input {
      width: 52px; text-align: center; font: inherit; padding: 6px 4px; border-radius: 8px;
      color: inherit; background: transparent; border: 1px solid color-mix(in srgb, currentColor 30%, transparent);
    }
    .subtotal { min-width: 70px; text-align: right; }
    .cliente { width: 100%; }
    .total { display: flex; justify-content: space-between; align-items: baseline; font-size: 24px; margin: 4px 0 12px; }
    .botones { display: flex; justify-content: space-between; gap: 8px; }
    .cobrar { flex: 1; height: 48px; font-size: 16px; }
    .exito {
      display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 12px 16px; margin-bottom: 16px;
      border-radius: 12px; background: #e6f4ea; color: #1e4d2b;
    }
    .exito-texto { display: flex; flex-direction: column; flex: 1; min-width: 160px; }
  `,
})
export class PuntoVenta {
  private readonly productosApi = inject(ProductosApi);
  private readonly inventarioApi = inject(InventarioApi);
  private readonly clientesApi = inject(ClientesApi);
  private readonly ventasApi = inject(VentasApi);
  private readonly notificador = inject(Notificador);
  private readonly confirmador = inject(Confirmador);

  private readonly buscador = viewChild<ElementRef<HTMLInputElement>>('buscador');

  protected readonly maxResultados = MAX_RESULTADOS;
  protected readonly texto = signal('');
  protected readonly buscando = signal(false);
  private readonly encontrados = signal<Producto[]>([]);
  protected readonly resultados = computed(() => this.encontrados().slice(0, MAX_RESULTADOS));
  protected readonly hayMas = computed(() => this.encontrados().length > MAX_RESULTADOS);

  private readonly stock = signal<ReadonlyMap<number, number>>(new Map());
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly clienteId = signal<number | null>(null);
  protected readonly carrito = signal<Linea[]>([]);
  protected readonly cobrando = signal(false);
  protected readonly abriendoBoleta = signal(false);
  protected readonly ultimaVenta = signal<Venta | null>(null);

  protected readonly unidades = computed(() => this.carrito().reduce((n, l) => n + l.cantidad, 0));
  protected readonly total = computed(
    () => this.carrito().reduce((suma, l) => suma + aCentimos(l.producto.precioVenta) * l.cantidad, 0) / 100,
  );

  /** Cada búsqueda cancela la anterior (switchMap) para que una respuesta lenta no pise a una más nueva. */
  private readonly busquedas = new Subject<{ texto: string; agregarSiEsUnico: boolean }>();

  constructor() {
    this.busquedas
      .pipe(
        debounceTime(250),
        tap(() => this.buscando.set(true)),
        switchMap(({ texto, agregarSiEsUnico }) =>
          this.productosApi.listar({ nombre: texto }).pipe(
            tap((productos) => {
              this.buscando.set(false);
              this.alLlegarResultados(texto, productos, agregarSiEsUnico);
            }),
            catchError((error: unknown) => {
              this.buscando.set(false);
              this.notificador.error(error, 'No se pudo buscar productos');
              return of<Producto[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe();

    this.busquedas.next({ texto: '', agregarSiEsUnico: false });
    this.cargarStock();
    this.clientesApi
      .listar()
      .pipe(catchError(() => of<Cliente[]>([])))
      .subscribe((clientes) => this.clientes.set(clientes.filter((c) => c.activo)));
  }

  protected stockDe(productoId: number): number {
    return this.stock().get(productoId) ?? 0;
  }

  private cargarStock(): void {
    this.inventarioApi.listar().subscribe({
      next: (filas) => this.stock.set(new Map(filas.map((s) => [s.productoId, s.cantidad]))),
      error: (error: unknown) => this.notificador.error(error, 'No se pudo cargar el stock'),
    });
  }

  protected escribir(valor: string): void {
    this.texto.set(valor);
    this.busquedas.next({ texto: valor.trim(), agregarSiEsUnico: false });
  }

  /** Enter (lector de código de barras o teclado): agrega el producto si el código coincide o es el único resultado. */
  protected enter(evento: Event): void {
    evento.preventDefault();
    const texto = this.texto().trim();
    if (texto === '') {
      return;
    }
    this.busquedas.next({ texto, agregarSiEsUnico: true });
  }

  private alLlegarResultados(texto: string, productos: Producto[], agregarSiEsUnico: boolean): void {
    this.encontrados.set(productos);
    if (!agregarSiEsUnico) {
      return;
    }
    const exacto = productos.find((p) => p.codigo.toLowerCase() === texto.toLowerCase());
    const elegido = exacto ?? (productos.length === 1 ? productos[0] : null);
    if (elegido) {
      this.agregar(elegido);
      this.texto.set('');
      this.busquedas.next({ texto: '', agregarSiEsUnico: false });
    } else if (productos.length === 0) {
      this.notificador.aviso(`No hay ningún producto con "${texto}"`);
    }
  }

  protected agregar(producto: Producto): void {
    const disponible = this.stockDe(producto.id);
    const actual = this.carrito().find((l) => l.producto.id === producto.id);
    const nueva = (actual?.cantidad ?? 0) + 1;
    if (nueva > disponible) {
      this.notificador.aviso(
        disponible <= 0
          ? `"${producto.nombre}" no tiene stock`
          : `Solo hay ${disponible} unidad(es) de "${producto.nombre}" en stock`,
      );
      return;
    }
    this.ultimaVenta.set(null);
    this.carrito.update((lineas) =>
      actual
        ? lineas.map((l) => (l.producto.id === producto.id ? { ...l, cantidad: nueva } : l))
        : [...lineas, { producto, cantidad: 1 }],
    );
    this.enfocarBuscador();
  }

  protected maximo(linea: Linea): number {
    return Math.min(MAX_CANTIDAD, Math.max(1, this.stockDe(linea.producto.id)));
  }

  protected cambiar(linea: Linea, cantidad: number, evento?: Event): void {
    if (!Number.isFinite(cantidad) || cantidad < 1) {
      // 0 o vacío: se interpreta como "quitar solo si se usó el botón menos"; en la caja de texto se restaura
      if (evento) {
        (evento.target as HTMLInputElement).value = String(linea.cantidad);
        return;
      }
      this.quitar(linea);
      return;
    }
    const entera = Math.floor(cantidad);
    const limite = this.maximo(linea);
    if (entera > limite) {
      this.notificador.aviso(`Solo hay ${this.stockDe(linea.producto.id)} unidad(es) de "${linea.producto.nombre}"`);
    }
    const final = Math.min(entera, limite);
    this.carrito.update((lineas) =>
      lineas.map((l) => (l.producto.id === linea.producto.id ? { ...l, cantidad: final } : l)),
    );
    if (evento) {
      (evento.target as HTMLInputElement).value = String(final);
    }
  }

  protected quitar(linea: Linea): void {
    this.carrito.update((lineas) => lineas.filter((l) => l.producto.id !== linea.producto.id));
  }

  protected subtotal(linea: Linea): number {
    return (aCentimos(linea.producto.precioVenta) * linea.cantidad) / 100;
  }

  protected vaciar(): void {
    this.confirmador
      .preguntar({ titulo: 'Vaciar carrito', mensaje: '¿Quitar todos los productos del carrito?', aceptar: 'Vaciar' })
      .subscribe((si) => {
        if (si) {
          this.carrito.set([]);
          this.clienteId.set(null);
          this.enfocarBuscador();
        }
      });
  }

  protected cobrar(): void {
    if (this.cobrando() || this.carrito().length === 0) {
      return;
    }
    this.cobrando.set(true);
    this.ventasApi
      .registrar({
        clienteId: this.clienteId(),
        items: this.carrito().map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad })),
      })
      .pipe(finalize(() => this.cobrando.set(false)))
      .subscribe({
        next: (venta) => {
          this.ultimaVenta.set(venta);
          this.carrito.set([]);
          this.clienteId.set(null);
          this.cargarStock();
          this.notificador.ok(`Venta ${venta.numero} registrada`);
          this.enfocarBuscador();
        },
        error: (error: unknown) => {
          // Normalmente 409 por stock: el mensaje del servidor dice cuál producto. Se actualiza el stock mostrado.
          this.notificador.error(error, 'No se pudo registrar la venta');
          this.cargarStock();
        },
      });
  }

  protected verBoleta(venta: Venta): void {
    this.abriendoBoleta.set(true);
    this.ventasApi
      .boleta(venta.id)
      .pipe(finalize(() => this.abriendoBoleta.set(false)))
      .subscribe({
        next: (pdf) => abrirPdf(pdf, `boleta-${venta.numero}.pdf`),
        error: (error: unknown) => this.notificador.error(error, 'No se pudo abrir la boleta'),
      });
  }

  private enfocarBuscador(): void {
    queueMicrotask(() => this.buscador()?.nativeElement.focus());
  }
}
