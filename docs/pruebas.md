# Pruebas automáticas

Las pruebas usan JUnit 5 y **H2 en memoria**: no necesitan MySQL, RabbitMQ ni Docker.
Hay **219 pruebas** en 7 servicios.

| Servicio | Pruebas |
|---|---|
| ventas-service | 67 |
| inventario-service | 64 |
| catalogo-service | 23 |
| api-gateway | 21 |
| compras-service | 18 |
| notificaciones-service | 16 |
| auth-service | 10 |

## Capa de acceso a datos: insertar, actualizar, eliminar y listar

Cada repositorio tiene su prueba `@DataJpaTest` con las cuatro operaciones:

| Repositorio | Insertar | Actualizar | Eliminar | Listar |
|---|:-:|:-:|:-:|:-:|
| `UsuarioRepository` (auth) | ✔ | ✔ | ✔ | ✔ |
| `CategoriaRepository` (catálogo) | ✔ | ✔ | ✔ | ✔ |
| `ProveedorRepository` (catálogo) | ✔ | ✔ | ✔ | ✔ |
| `ProductoRepository` (catálogo) | ✔ | ✔ | ✔ | ✔ |
| `StockRepository` (inventario) | ✔ | ✔ | ✔ | ✔ |
| `MovimientoRepository` (inventario) | ✔ | ✔ | ✔ | ✔ (kardex) |
| `VentaRepository` (ventas) | ✔ | ✔ | ✔ | ✔ (por rango) |
| `ClienteRepository` (ventas) | ✔ | ✔ | ✔ | ✔ |
| `CompraRepository` (compras) | ✔ | ✔ | ✔ | ✔ |
| `NotificacionRepository` (notificaciones) | ✔ | ✔ (marcar leídas) | ✔ | ✔ |

Además hay pruebas de reglas de la base (claves únicas duplicadas, cascada de detalle) y de servicios,
mensajería, boleta PDF, clientes Feign, rutas, CORS y JWT del gateway.

## Cómo ejecutarlas

```powershell
.\gradlew test                       # todos los servicios
.\gradlew :ventas-service:test       # uno solo
```

El informe HTML queda en `<servicio>/build/reports/tests/test/index.html`.

## En GitHub Actions

El workflow **Pruebas** (`.github/workflows/pruebas.yml`) las ejecuta en cada push y pull request.
En la pestaña *Actions* se ve el resumen por clase y se puede descargar el informe HTML
(artefacto `informes-de-pruebas`).
