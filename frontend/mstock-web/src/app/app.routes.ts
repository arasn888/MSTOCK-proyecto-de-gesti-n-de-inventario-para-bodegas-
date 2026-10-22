import { Route, Routes } from '@angular/router';
import { authGuard, invitadoGuard, rolGuard } from './core/auth/guards';
import { MENU } from './core/auth/navegacion';

const enConstruccion = () =>
  import('./pages/en-construccion/en-construccion').then((m) => m.EnConstruccion);

/**
 * Pantallas ya implementadas, por ruta del menú. Cada módulo que se construye se agrega aquí; las que
 * faltan muestran "en construcción" (con sus permisos por rol ya aplicados).
 */
const implementadas: Partial<Record<string, Route['loadComponent']>> = {
  '/categorias': () => import('./pages/categorias/categorias').then((m) => m.Categorias),
  '/proveedores': () => import('./pages/proveedores/proveedores').then((m) => m.Proveedores),
  '/inventario': () => import('./pages/inventario/inventario').then((m) => m.Inventario),
  '/notificaciones': () => import('./pages/notificaciones/notificaciones').then((m) => m.Notificaciones),
  '/clientes': () => import('./pages/clientes/clientes').then((m) => m.Clientes),
  '/cierre-caja': () => import('./pages/cierre-caja/cierre-caja').then((m) => m.CierreCajaPagina),
  '/compras': () => import('./pages/compras/compras').then((m) => m.Compras),
  '/usuarios': () => import('./pages/usuarios/usuarios').then((m) => m.Usuarios),
  '/reportes': () => import('./pages/dashboard/dashboard').then((m) => m.DashboardPagina),
  '/ventas': () => import('./pages/ventas/ventas').then((m) => m.Ventas),
  '/ventas/nueva': () => import('./pages/ventas/punto-venta').then((m) => m.PuntoVenta),
  '/productos': () => import('./pages/productos/productos').then((m) => m.Productos),
};

const pantallasDelMenu: Routes = MENU.filter((item) => item.ruta !== '/inicio').map((item) => ({
  path: item.ruta.slice(1),
  canActivate: [rolGuard(...item.roles)],
  loadComponent: implementadas[item.ruta] ?? enConstruccion,
  data: { titulo: item.etiqueta },
}));

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/inicio/inicio').then((m) => m.Inicio),
      },
      ...pantallasDelMenu,
    ],
  },
  { path: '**', redirectTo: '' },
];
