import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Título de la pantalla con un espacio a la derecha para los botones de acción (contenido proyectado). */
@Component({
  selector: 'app-cabecera-pagina',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <h1>{{ titulo() }}</h1>
        @if (subtitulo()) {
          <p class="sub">{{ subtitulo() }}</p>
        }
      </div>
      <div class="acciones"><ng-content /></div>
    </header>
  `,
  styles: `
    .cabecera { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
    h1 { margin: 0; font-size: 24px; font-weight: 500; }
    .sub { margin: 4px 0 0; opacity: 0.7; }
    .acciones { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
  `,
})
export class CabeceraPagina {
  readonly titulo = input.required<string>();
  readonly subtitulo = input('');
}
