import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';

/** Marcador de las pantallas que aún no se construyen (los permisos por rol ya funcionan). */
@Component({
  selector: 'app-en-construccion',
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <mat-icon>construction</mat-icon>
      <h1>{{ titulo }}</h1>
      <p>Esta pantalla se construye en una de las siguientes entregas.</p>
    </section>
  `,
  styles: `
    .pagina { padding: 48px 24px; text-align: center; opacity: 0.8; }
    mat-icon { font-size: 48px; width: 48px; height: 48px; }
  `,
})
export class EnConstruccion {
  protected readonly titulo = String(inject(ActivatedRoute).snapshot.data['titulo'] ?? 'Pantalla');
}
