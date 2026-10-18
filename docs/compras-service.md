# compras-service

Microservicio de compras añadido a MSTOCK. Usa Gradle, Java 21, MySQL, Flyway, Eureka, Feign y RabbitMQ.

## Endpoints

Requieren un JWT válido con rol `ADMIN` o `ALMACENERO`; el `CAJERO` recibe 403. Los errores usan el mismo formato `{estado, mensaje, fecha}` que el resto de los servicios.

- `POST /api/compras`: registra una compra y sus detalles.
- `GET /api/compras`: lista compras.
- `GET /api/compras/{id}`: consulta una compra.

Ejemplo de `POST /api/compras` (el precio de compra se toma del catálogo, no del cliente):

```json
{
  "proveedorId": 1,
  "items": [
    { "productoId": 1, "cantidad": 12 },
    { "productoId": 2, "cantidad": 6 }
  ]
}
```

El proveedor y cada producto se validan contra `catalogo-service`. Si el producto tiene proveedor asignado, debe coincidir con el proveedor de la compra. La compra guarda el precio de compra vigente en el catálogo como costo unitario.

Estados de la compra: `PENDIENTE` (recién guardada), `RECIBIDA` (inventario sumó el stock) y `RECHAZADA` (inventario no pudo; el motivo queda en `motivoRechazo`). El cambio llega por los eventos `compra.aplicada` / `compra.rechazada` que publica inventario. Las llamadas a catálogo se hacen antes y fuera de la transacción; solo el guardado final es transaccional.

Después de confirmar la transacción, `compras-service` publica `compra.registrada` en el exchange `mstock.eventos`. `inventario-service` consume el evento, registra las entradas y agrega los movimientos al kardex. La tabla `compras_procesadas` evita aplicar dos veces un mismo evento cuando RabbitMQ lo entrega más de una vez. El movimiento del kardex queda a nombre del usuario que registró la compra. Si el inventario no puede aplicar el evento tras 3 intentos, el mensaje va a la cola `inventario.dlq`.

## Arranque

Con las variables de `.env` cargadas y los servicios de infraestructura levantados:

```powershell
.\gradlew.bat :compras-service:bootRun
```

El API Gateway expone las rutas mediante `/api/compras/**`.

## Importante

La publicación se hace después del commit y los errores quedan registrados en el log. Esta versión todavía no usa outbox transaccional: si RabbitMQ está desconectado justo al publicar, la compra permanece registrada pero el evento requiere revisión/reintento operativo.

## Nota sobre migraciones de inventario

La migración `V99__registrar_compras_procesadas.sql` de inventario-service tiene un número alto. Flyway no acepta migraciones con número menor que la última aplicada, por eso la **próxima migración de inventario debe llamarse `V100__...`** (a menos que se recree la base de datos).
