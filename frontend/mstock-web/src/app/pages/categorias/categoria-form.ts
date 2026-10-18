import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs';
import { CategoriasApi } from '../../core/api/categorias.api';
import { Categoria, CategoriaRequest } from '../../core/models/catalogo';
import { Notificador } from '../../core/notificador.service';
import { vacioANull } from '../../core/util/texto';

/** Crear (data = null) o editar (data = la categoría) una categoría. Cierra con true si se guardó. */
@Component({
  selector: 'app-categoria-form',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatButtonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ categoria ? 'Editar categoría' : 'Nueva categoría' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos">
        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" maxlength="80" autofocus />
          @if (form.controls.nombre.hasError('required')) {
            <mat-error>El nombre es obligatorio</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Descripción (opcional)</mat-label>
          <textarea matInput formControlName="descripcion" rows="3" maxlength="255"></textarea>
        </mat-form-field>
        <mat-slide-toggle formControlName="activo">Activa</mat-slide-toggle>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" [mat-dialog-close]="false" [disabled]="guardando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .campos { display: flex; flex-direction: column; gap: 4px; min-width: min(420px, 80vw); }
  `,
})
export class CategoriaForm {
  protected readonly categoria = inject<Categoria | null>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<CategoriaForm, boolean>>(MatDialogRef);
  private readonly api = inject(CategoriasApi);
  private readonly notificador = inject(Notificador);

  protected readonly guardando = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    nombre: [this.categoria?.nombre ?? '', [Validators.required, Validators.maxLength(80)]],
    descripcion: [this.categoria?.descripcion ?? '', Validators.maxLength(255)],
    activo: [this.categoria?.activo ?? true],
  });

  protected guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const valores = this.form.getRawValue();
    const datos: CategoriaRequest = {
      nombre: valores.nombre.trim(),
      descripcion: vacioANull(valores.descripcion),
      activo: valores.activo,
    };
    this.guardando.set(true);
    const peticion = this.categoria
      ? this.api.actualizar(this.categoria.id, datos)
      : this.api.crear(datos);
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.notificador.ok(this.categoria ? 'Categoría actualizada' : 'Categoría creada');
        this.ref.close(true);
      },
      error: (error: unknown) => this.notificador.error(error, 'No se pudo guardar la categoría'),
    });
  }
}
