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

## API Gateway (puerto 8080)
Toda petición del frontend entra por `http://localhost:8080`; los puertos de cada servicio quedan solo para desarrollo.
- Rutas: `/api/auth` y `/api/usuarios` -> auth; `/api/productos`, `/api/categorias` y `/api/proveedores` -> catálogo;
  `/api/inventario` -> inventario; `/api/ventas` y `/api/clientes` -> ventas; `/api/compras` -> compras; `/api/notificaciones` -> notificaciones. Se resuelven por Eureka (`lb://`).
- Seguridad: solo `POST /api/auth/login` y `/actuator/health` son públicos. El gateway rechaza con 401 lo que no traiga
  un JWT válido (misma clave y emisor que auth). Los permisos por rol (403) los siguen decidiendo los servicios,
  que también validan el token: saltarse el gateway no da acceso.
- Resiliencia: cada ruta tiene un circuit breaker; si el servicio no responde, el gateway contesta 503 en JSON.
  Un 503 que el propio servicio devuelve con su mensaje (ventas avisando que el inventario cayó) pasa sin cambios.

## Pruebas
    .\gradlew.bat test
    .\gradlew.bat :ventas-service:test
Pruebas de la API: importar en Postman las colecciones de `docs/postman/` y ejecutarlas con el Runner
(`MSTOCK-gateway` prueba todo por el puerto 8080; `MSTOCK-anulacion` prueba la anulación y la idempotencia del inventario).
La carpeta de resiliencia de cada colección es manual (requiere apagar un servicio).

## Pendiente
Reportes (dashboard, cierre de caja, boleta PDF, kardex exportable; deben excluir las ventas `ANULADA`), CORS en el gateway,
Angular, Docker/Kubernetes y observabilidad. Mejoras anotadas: outbox para no perder eventos (compras y ventas), usar la
cuenta de servicio también en el descuento de stock (hoy va con el token del cajero) y una clave de idempotencia enviada
por el cliente para que un doble clic en "Vender" no cree dos ventas.
