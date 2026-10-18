export type TipoNotificacion = 'STOCK_BAJO' | 'VENTA_REGISTRADA' | 'VENTA_ANULADA';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje: string;
  /** Id del producto o de la venta que originó el aviso. */
  referenciaId: number | null;
  leida: boolean;
  fecha: string;
}

export interface ResumenNotificaciones {
  noLeidas: number;
}

export interface LecturaMasiva {
  actualizadas: number;
}
