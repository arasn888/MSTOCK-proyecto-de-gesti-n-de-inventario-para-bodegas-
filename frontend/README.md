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

Después **descomprime encima del proyecto** el zip de la entrega (acepta reemplazar `app.ts`, `app.config.ts` y `app.routes.ts`)
y borra los archivos del scaffold que ya no se usan:

```powershell
del src\app\app.html, src\app\app.scss, src\app\app.spec.ts
```

## Ejecutar

```powershell
# Backend encendido: eureka, config-server, auth, catalogo, inventario, ventas, compras, notificaciones y api-gateway
ng serve
```

Abre `http://localhost:4200`. Compilar sin servir: `ng build`.

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
  pages/              login, inicio y "en construcción" (se reemplaza módulo a módulo)
```

## Seguridad en el frontend

- El **interceptor** adjunta `Authorization: Bearer <token>` solo a peticiones hacia el gateway y, ante un 401, cierra la sesión y
  manda al login.
- Los **guards** (`authGuard`, `rolGuard`) y el menú ocultan lo que el rol no puede usar. Es comodidad, no seguridad: la barrera
  real son los servicios, que validan el JWT y el rol en cada petición.
- Los permisos del menú reflejan los de los servicios: ADMIN todo; CAJERO ventas, clientes, cierre de caja y consulta de
  productos; ALMACENERO catálogo (lectura), inventario, compras y notificaciones.

## Cómo agregar una pantalla

1. Crea el componente en `src/app/pages/<modulo>/`.
2. Regístralo en `implementadas` de `app.routes.ts` con la ruta del menú (`'/productos': () => import(...).then(m => m.Productos)`).
   Los permisos por rol ya vienen de `core/auth/navegacion.ts`.
