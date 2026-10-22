# MSTOCK

Sistema de gestión de inventario y ventas para bodegas y minimarkets, construido con arquitectura de
microservicios (una base de datos por servicio).

## Stack
Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.2, Gradle, MySQL 8.4 (Docker), Feign y Resilience4J.

## Módulos
| Módulo | Puerto | Base de datos | Descripción |
|---|---|---|---|
| config-server | 9888 | - | Configuración centralizada |
| eureka-server | 9761 | - | Registro y descubrimiento de servicios |
| auth-service | 9001 | mstock_auth | Usuarios, login (BCrypt + JWT) y roles |
| catalogo-service | 9002 | mstock_catalogo | Productos, categorías y proveedores |
| inventario-service | 9003 | mstock_inventario | Stock, kardex y alertas; valida productos con el catálogo (Feign) |
| ventas-service | 9004 | mstock_ventas | Ventas y clientes; precio del catálogo y descuento de stock del inventario; publica `venta.registrada` |
| compras-service | 9005 | mstock_compras | Compras a proveedores (ADMIN y ALMACENERO): costos del catálogo; publica `compra.registrada` |
| notificaciones-service | 9006 | mstock_notificaciones | Consume eventos de RabbitMQ (`stock.bajo`, `venta.registrada`) y los guarda como notificaciones |
| api-gateway | 8080 | - | Entrada única: valida el JWT y enruta a los servicios (Eureka); circuit breaker por ruta |

Roles: `ADMIN`, `ALMACENERO`, `CAJERO`. Todos los servicios validan el mismo JWT.
Entre servicios, ventas usa un token de corta duración con rol interno `SERVICIO` para devolver stock al compensar o anular una venta (`POST /api/inventario/reversiones` y `POST /api/inventario/operaciones/{id}/reversion` solo los aceptan `SERVICIO` y `ADMIN`).
RabbitMQ (docker compose, puerto 5673, panel 15673): inventario publica `stock.bajo`, ventas `venta.registrada` y `venta.anulada` y compras `compra.registrada` (el inventario la consume, suma el stock y responde `compra.aplicada` o `compra.rechazada`, que pasa la compra a RECIBIDA o RECHAZADA; si falla 3 veces va a `inventario.dlq`); ver `docs/eventos-rabbitmq.md` y `docs/compras-service.md`. Si RabbitMQ está caído, la operación igual se completa y el evento se pierde (queda un aviso en el log).

## Preparación
1. Copiar `.env.example` como `.env` y completar las claves (`.env` no se sube a Git).
2. Levantar MySQL: `docker compose up -d` (queda en `localhost:3307`).
3. En cada terminal nueva, cargar las claves: `Get-Content .\scripts\cargar-env.ps1 -Raw | Invoke-Expression`

## Cómo ejecutar (en este orden, una terminal por servicio)
    .\gradlew.bat :eureka-server:bootRun
    .\gradlew.bat :config-server:bootRun
    .\gradlew.bat :auth-service:bootRun
    .\gradlew.bat :catalogo-service:bootRun
    .\gradlew.bat :inventario-service:bootRun
    .\gradlew.bat :ventas-service:bootRun
    .\gradlew.bat :compras-service:bootRun
    .\gradlew.bat :notificaciones-service:bootRun
    .\gradlew.bat :api-gateway:bootRun

Eureka: http://localhost:9761. Usuario inicial: `admin` (contraseña en `ADMIN_PASSWORD`; por defecto `Admin123*`,
solo para desarrollo).

## Flujo de una venta
`POST /api/ventas` en ventas-service:
1. Toma el precio vigente de cada producto del catálogo (Feign; el cliente nunca decide el precio).
2. Genera un **id de operación** (UUID) y descuenta el stock en el inventario con una sola operación todo-o-nada (Feign).
3. Guarda la venta y su detalle (con ese id de operación). Si guardar falla después de descontar, deshace el descuento por id (compensación).

Resiliencia: el catálogo se reintenta (lectura) y tiene circuit breaker. El inventario también tiene circuit breaker y
se reintenta **una vez** (solo ante timeout o 5xx) porque el descuento es idempotente: el inventario guarda el id de
operación y, si llega repetido, devuelve el resultado de la primera vez sin descontar otra vez. Si tras reintentar no se
sabe si descontó, la venta se rechaza con 503 y se deshace por id (no devuelve nada si el descuento nunca se aplicó, y
si el descuento llega tarde con ese id, el inventario lo rechaza). Si el servicio está caído (circuito abierto), 503.

## Anulación de ventas
`POST /api/ventas/{id}/anulacion` con `{ "motivo": "..." }` (solo ADMIN):
1. Devuelve el stock por id de operación (`POST /api/inventario/operaciones/{id}/reversion`, cuenta de servicio): devuelve
   exactamente lo que se descontó y es idempotente.
2. Marca la venta `ANULADA` (con motivo, usuario y fecha) con un `UPDATE ... WHERE estado = 'COMPLETADA'`, y publica `venta.anulada`.

Primero el stock y luego el estado: si algo falla en medio la venta sigue `COMPLETADA` y se puede reintentar sin riesgo.
Anular una venta ya anulada devuelve su estado actual (200) sin tocar el inventario. Las ventas anteriores al control de
operaciones (sin id) se anulan devolviendo por lista de productos con la clave fija `ANUL-<id>`.

## CORS (frontend Angular)
Solo el gateway lo configura (los servicios internos no reciben tráfico del navegador). Está en la cadena de seguridad, antes de la
autenticación, porque el preflight `OPTIONS` del navegador no lleva token y de otro modo recibiría 401.
- Orígenes permitidos: `mstock.cors.allowed-origins` (variable de entorno `CORS_ALLOWED_ORIGINS`, separados por coma). Por defecto
  `http://localhost:4200` y `http://127.0.0.1:4200`. No admite `*`: el gateway no arranca si está vacío o lo contiene.
- Métodos `GET, POST, PUT, DELETE, OPTIONS`; cabeceras `Authorization, Content-Type, Accept`; sin credenciales (el JWT va en la
  cabecera, no en cookies). Expone `Content-Disposition` (nombre de la boleta y del kardex descargados) y `Location`.
- Solo aplica a `/api/**`. En producción, pon el dominio real del frontend en `CORS_ALLOWED_ORIGINS`.
- Angular debe llamar siempre al gateway (puerto 8080), nunca directo a los servicios.

## Reportes (ventas-service)
Solo cuentan las ventas `COMPLETADAS`; las anuladas se informan aparte. Rango de fechas por día (ambos incluidos), sin fechas = hoy.
- `GET /api/reportes/dashboard?desde=&hasta=&top=` (solo ADMIN): ventas (cantidad, total, ticket promedio), ganancia, anuladas y
  los `top` productos más vendidos (por defecto 5, máximo 50; rango máximo 366 días). El **stock crítico** no está aquí: es del
  inventario, `GET /api/inventario/alertas`.
- `GET /api/reportes/cierre-caja?fecha=` (ADMIN y CAJERO): ventas y anulaciones por cajero según la fecha de la venta. El ADMIN
  ve a todos; un CAJERO solo ve su propia fila.
- Ganancia = suma de (precio - costo) x cantidad. El **costo se guarda en cada línea al vender** (el catálogo puede cambiar
  después). Las ventas anteriores a este cambio no tienen costo: no entran en la ganancia y se cuentan en `lineasSinCosto`
  (si es mayor que 0, la ganancia está subestimada). El costo nunca se muestra en el detalle de la venta que ve el cajero.

## Boleta en PDF y kardex en CSV
- `GET /api/ventas/{id}/boleta`: boleta simulada en PDF (A6) con negocio, número, fecha, cajero, cliente, productos y total. Las ventas
  anuladas se imprimen con la marca "VENTA ANULADA" y su motivo. No lleva el costo de los productos. El nombre del negocio sale de
  `mstock.boleta.negocio` (por defecto `MSTOCK`). La puede pedir cualquier usuario autenticado, igual que el detalle de la venta.
- `GET /api/inventario/{productoId}/kardex/csv` (ADMIN y ALMACENERO): kardex del producto del más antiguo al más reciente, en CSV UTF-8
  con BOM (se abre en Excel con las tildes bien). Las comas y comillas se escapan y los motivos que empiezan con `= + - @` se
  neutralizan para que Excel no los ejecute como fórmula. Si el producto no existe en el catálogo responde 404.

## API Gateway (puerto 8080)
Toda petición del frontend entra por `http://localhost:8080`; los puertos de cada servicio quedan solo para desarrollo.
- Rutas: `/api/auth` y `/api/usuarios` -> auth; `/api/productos`, `/api/categorias` y `/api/proveedores` -> catálogo;
  `/api/inventario` -> inventario; `/api/ventas`, `/api/clientes` y `/api/reportes` -> ventas; `/api/compras` -> compras; `/api/notificaciones` -> notificaciones. Se resuelven por Eureka (`lb://`).
- Seguridad: solo `POST /api/auth/login` y `/actuator/health` son públicos. El gateway rechaza con 401 lo que no traiga
  un JWT válido (misma clave y emisor que auth). Los permisos por rol (403) los siguen decidiendo los servicios,
  que también validan el token: saltarse el gateway no da acceso.
- Resiliencia: cada ruta tiene un circuit breaker; si el servicio no responde, el gateway contesta 503 en JSON.
  Un 503 que el propio servicio devuelve con su mensaje (ventas avisando que el inventario cayó) pasa sin cambios.

## Pruebas
    .\gradlew.bat test
    .\gradlew.bat :ventas-service:test
Pruebas de la API: importar en Postman las colecciones de `docs/postman/` y ejecutarlas con el Runner
(`MSTOCK-gateway` prueba todo por el puerto 8080; `MSTOCK-anulacion` prueba la anulación y la idempotencia del inventario; `MSTOCK-reportes` prueba el dashboard y el cierre de caja; `MSTOCK-boleta-kardex` prueba la boleta PDF y el kardex CSV; `MSTOCK-cors` prueba el preflight y las cabeceras CORS contra el gateway).
La carpeta de resiliencia de cada colección es manual (requiere apagar un servicio).

## Frontend (Angular)
En `frontend/` (ver `frontend/README.md`): Angular 22 con Angular Material, consumiendo todos los servicios solo a través del
gateway. Incluye todas las pantallas del menú (punto de venta, ventas, clientes, cierre de caja, productos, categorías, proveedores,
inventario, compras, notificaciones, dashboard y usuarios), el login con JWT, el interceptor y los guards por rol.
Permisos por pantalla y pruebas manuales: `docs/frontend.md`.

## Pendiente

Docker/Kubernetes y observabilidad (Fase 7); la URL del gateway del frontend hoy está fija en `environment.ts`. Mejoras anotadas: outbox para no perder eventos (compras y ventas), usar la
cuenta de servicio también en el descuento de stock (hoy va con el token del cajero) y una clave de idempotencia enviada
por el cliente para que un doble clic en "Vender" no cree dos ventas.
