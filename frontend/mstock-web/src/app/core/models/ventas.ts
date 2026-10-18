export type EstadoVenta = 'COMPLETADA' | 'ANULADA';

export interface Cliente {
  id: number;
  documento: string | null;
  nombres: string;
  telefono: string | null;
  email: string | null;
  activo: boolean;
}

export interface ClienteRequest {
  /** DNI (8 dígitos) o RUC (11 dígitos); puede ir vacío. */
  documento?: string | null;
  nombres: string;
  telefono?: string | null;
  email?: string | null;
}

/** El cliente solo indica producto y cantidad: el precio lo pone siempre el catálogo. */
export interface ItemVentaRequest {
  productoId: number;
  cantidad: number;
}

export interface VentaRequest {
  clienteId?: number | null;
  items: ItemVentaRequest[];
}

export interface DetalleVenta {
  productoId: number;
  codigo: string;
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Venta {
  id: number;
  numero: string;
  clienteId: number | null;
  clienteNombre: string | null;
  usuario: string;
  fecha: string;
  total: number;
  estado: EstadoVenta;
  motivoAnulacion: string | null;
  fechaAnulacion: string | null;
  usuarioAnulacion: string | null;
  detalle: DetalleVenta[];
}

/** Fila del listado: sin el detalle. */
export interface VentaResumen {
  id: number;
  numero: string;
  clienteNombre: string | null;
  usuario: string;
  fecha: string;
  total: number;
  estado: EstadoVenta;
}

export interface FiltroVentas {
  /** Fecha ISO (yyyy-MM-dd). */
  desde?: string;
  hasta?: string;
}

export interface Dashboard {
  desde: string;
  hasta: string;
  ventas: {
    cantidad: number;
    total: number;
    ticketPromedio: number;
    ganancia: number;
    /** Líneas de ventas antiguas sin costo: si es mayor que 0, la ganancia está subestimada. */
    lineasSinCosto: number;
  };
  anuladas: { cantidad: number; total: number };
  masVendidos: {
    productoId: number;
    codigo: string;
    nombre: string;
    unidadesVendidas: number;
    ingresos: number;
  }[];
}

export interface CierreCajaCajero {
  usuario: string;
  cantidadVentas: number;
  totalVendido: number;
  cantidadAnuladas: number;
  totalAnulado: number;
}

export interface CierreCaja {
  fecha: string;
  cajeros: CierreCajaCajero[];
  totales: {
    cantidadVentas: number;
    totalVendido: number;
    cantidadAnuladas: number;
    totalAnulado: number;
  };
}
