export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}

export interface CategoriaRequest {
  nombre: string;
  descripcion?: string | null;
  activo?: boolean;
}

export interface Proveedor {
  id: number;
  ruc: string;
  razonSocial: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  activo: boolean;
}

export interface ProveedorRequest {
  ruc: string;
  razonSocial: string;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  activo?: boolean;
}

export interface Producto {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  precioCompra: number;
  precioVenta: number;
  unidadMedida: string | null;
  categoriaId: number;
  categoriaNombre: string;
  proveedorId: number | null;
  proveedorNombre: string | null;
  activo: boolean;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface ProductoRequest {
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  precioCompra: number;
  precioVenta: number;
  unidadMedida?: string | null;
  categoriaId: number;
  proveedorId?: number | null;
  activo?: boolean;
}

export interface FiltroProductos {
  nombre?: string;
  categoriaId?: number;
}
