export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE';

export interface Stock {
  id: number;
  productoId: number;
  cantidad: number;
  stockMinimo: number;
  bajoMinimo: boolean;
  fechaActualizacion: string;
}

export interface Movimiento {
  id: number;
  productoId: number;
  tipo: TipoMovimiento;
  cantidad: number;
  stockAnterior: number;
  stockResultante: number;
  motivo: string | null;
  usuario: string;
  fecha: string;
}

export interface MovimientoRequest {
  productoId: number;
  cantidad: number;
  motivo?: string | null;
}

export interface AjusteRequest {
  productoId: number;
  /** Conteo físico real: el servidor calcula la diferencia contra el stock actual. */
  cantidadReal: number;
  motivo: string;
}
