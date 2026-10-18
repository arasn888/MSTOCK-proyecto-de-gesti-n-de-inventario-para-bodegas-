import { Rol } from '../models/auth';

export interface ItemMenu {
  ruta: string;
  etiqueta: string;
  icono: string;
  /** Roles que ven y pueden abrir esta pantalla (alineado con los permisos reales de los servicios). */
  roles: readonly Rol[];
}

const TODOS: readonly Rol[] = ['ADMIN', 'CAJERO', 'ALMACENERO'];

/** Única fuente de verdad del menú y de los guards de ruta. */
export const MENU: readonly ItemMenu[] = [
  { ruta: '/inicio', etiqueta: 'Inicio', icono: 'home', roles: TODOS },
  { ruta: '/ventas/nueva', etiqueta: 'Punto de venta', icono: 'point_of_sale', roles: ['ADMIN', 'CAJERO'] },
  { ruta: '/ventas', etiqueta: 'Ventas', icono: 'receipt_long', roles: ['ADMIN', 'CAJERO'] },
  { ruta: '/clientes', etiqueta: 'Clientes', icono: 'groups', roles: ['ADMIN', 'CAJERO'] },
  { ruta: '/cierre-caja', etiqueta: 'Cierre de caja', icono: 'payments', roles: ['ADMIN', 'CAJERO'] },
  { ruta: '/productos', etiqueta: 'Productos', icono: 'inventory_2', roles: TODOS },
  { ruta: '/categorias', etiqueta: 'Categorías', icono: 'category', roles: ['ADMIN', 'ALMACENERO'] },
  { ruta: '/proveedores', etiqueta: 'Proveedores', icono: 'local_shipping', roles: ['ADMIN', 'ALMACENERO'] },
  { ruta: '/inventario', etiqueta: 'Inventario', icono: 'warehouse', roles: ['ADMIN', 'ALMACENERO'] },
  { ruta: '/compras', etiqueta: 'Compras', icono: 'shopping_cart', roles: ['ADMIN', 'ALMACENERO'] },
  { ruta: '/notificaciones', etiqueta: 'Notificaciones', icono: 'notifications', roles: ['ADMIN', 'ALMACENERO'] },
  { ruta: '/reportes', etiqueta: 'Dashboard', icono: 'insights', roles: ['ADMIN'] },
  { ruta: '/usuarios', etiqueta: 'Usuarios', icono: 'manage_accounts', roles: ['ADMIN'] },
];

export function menuPara(rol: Rol | null): ItemMenu[] {
  return rol === null ? [] : MENU.filter((item) => item.roles.includes(rol));
}
