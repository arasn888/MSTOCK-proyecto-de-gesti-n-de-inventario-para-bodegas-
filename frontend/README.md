# MSTOCK Web (Angular 22 + Angular Material)

Frontend que consume **todos** los servicios REST a través del api-gateway (`http://localhost:8080`). Nunca llama
directo a un servicio: el gateway valida el JWT y ya tiene CORS para `http://localhost:4200`.

## Crear el proyecto (una sola vez)

Angular 22 es la versión estable activa. Necesitas el Node que exige Angular 22 (`ng new` te avisa si no coincide).

```powershell
npm install -g @angular/cli@latest
cd frontend
ng new mstock-web --style=scss --routing --ssr=false --skip-git
cd mstock-web
ng add @angular/material
```

- En `ng new`, responde que **no** a SSR/prerendering y deja el resto por defecto.
- En `ng add @angular/material`, elige el tema que prefieras. Debe agregar la fuente *Material Icons* a `src/index.html`; si los
  iconos salen como texto, agrega a mano en el `<head>`:
  `<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">`

Después copia encima del proyecto el código de `src/` del repositorio (acepta reemplazar `app.ts`, `app.config.ts` y `app.routes.ts`)
y borra los archivos del scaffold que ya no se usan:

```powershell
del src\app\app.html, src\app\app.scss, src\app\app.spec.ts
```

## Ejecutar

```powershell
# Backend encendido: eureka, config-server, auth, catalogo, inventario, ventas, compras, notificaciones y api-gateway
ng serve
```

Abre `http://localhost:4200`. Importante: `ng serve` se ejecuta **dentro** de `frontend/mstock-web` (donde está `angular.json`);
desde otra carpeta, Angular responde "command not available outside a workspace".

Compilar sin servir: `ng build` (desarrollo) o `ng build --configuration production` (optimizado, deja el resultado en
`dist/mstock-web/browser`; unos 120 kB transferidos en la carga inicial, el resto se descarga por pantalla).

## Estructura

```
src/environments/environment.ts   URL del gateway
src/app/
  core/models/        interfaces TypeScript = DTOs de cada servicio
  core/api/           un servicio por recurso (auth, usuarios, categorias, proveedores, productos, inventario,
                      clientes, ventas, reportes, compras, notificaciones). Cubren todos los endpoints.
  core/auth/          sesión (JWT en localStorage), interceptor, guards por rol, menú
  core/http/          mensajes de error de la API y descarga de archivos (boleta PDF, kardex CSV)
  layout/shell.ts     barra superior, menú lateral por rol, campana de notificaciones
  pages/              una carpeta por pantalla (ver la tabla de abajo); "en construcción" queda solo como respaldo
  shared/             piezas reutilizables: cabecera de página y diálogo de confirmación
```

## Seguridad en el frontend

- El **interceptor** adjunta `Authorization: Bearer <token>` solo a peticiones hacia el gateway y, ante un 401, cierra la sesión y
  manda al login.
- Los **guards** (`authGuard`, `rolGuard`) y el menú ocultan lo que el rol no puede usar. Es comodidad, no seguridad: la barrera
  real son los servicios, que validan el JWT y el rol en cada petición.
- Los permisos del menú reflejan los de los servicios: ADMIN todo; CAJERO ventas, clientes, cierre de caja y consulta de
  productos; ALMACENERO catálogo (lectura), inventario, compras y notificaciones.

## Pantallas

| Menú | Ruta | Roles | Qué hace |
|---|---|---|---|
| Punto de venta | `/ventas/nueva` | ADMIN, CAJERO | Busca por nombre o código (lector de código de barras + Enter), carrito, cliente opcional, cobra y abre la boleta PDF |
| Ventas | `/ventas` | ADMIN, CAJERO | Historial por fechas, detalle, boleta; solo ADMIN anula (con motivo) |
| Clientes | `/clientes` | ADMIN, CAJERO | Alta y edición; eliminar solo ADMIN |
| Cierre de caja | `/cierre-caja` | ADMIN, CAJERO | Totales del día; el CAJERO ve solo lo suyo |
| Productos | `/productos` | todos | Consulta; crear, editar y desactivar solo ADMIN |
| Categorías, Proveedores | `/categorias`, `/proveedores` | ADMIN, ALMACENERO | Consulta; escribir solo ADMIN |
| Inventario | `/inventario` | ADMIN, ALMACENERO | Stock, alertas, entradas, salidas, ajustes, stock mínimo, kardex y CSV |
| Compras | `/compras` | ADMIN, ALMACENERO | Registrar compras y ver su estado (PENDIENTE → RECIBIDA o RECHAZADA) |
| Notificaciones | `/notificaciones` | ADMIN, ALMACENERO | Avisos de stock bajo y de ventas; la campana muestra las no leídas |
| Dashboard | `/reportes` | ADMIN | Ventas, ticket promedio, ganancia, anuladas y productos más vendidos |
| Usuarios | `/usuarios` | ADMIN | Alta, edición, desactivación y eliminación de cuentas |

Matriz completa de permisos y lista de pruebas manuales por rol: `docs/frontend.md`.

## Cómo agregar una pantalla

1. Crea el componente en `src/app/pages/<modulo>/`.
2. Regístralo en `implementadas` de `app.routes.ts` con la ruta del menú (`'/productos': () => import(...).then(m => m.Productos)`).
   Los permisos por rol ya vienen de `core/auth/navegacion.ts`.
