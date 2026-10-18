export type EstadoCompra = 'PENDIENTE' | 'RECIBIDA' | 'RECHAZADA';

export interface DetalleCompraRequest {
  productoId: number;
  cantidad: number;
}

export interface CompraRequest {
  proveedorId: number;
  items: DetalleCompraRequest[];
}

export interface DetalleCompra {
  id: number;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
}

export interface Compra {
  id: number;
  proveedorId: number;
  usuario: string;
  fecha: string;
  total: number;
  /** PENDIENTE hasta que el inventario confirma el ingreso (asíncrono, por RabbitMQ). */
  estado: EstadoCompra;
  motivoRechazo: string | null;
  items: DetalleCompra[];
}
