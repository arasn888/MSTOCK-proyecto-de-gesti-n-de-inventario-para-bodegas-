# Eventos de MSTOCK (RabbitMQ)

Los servicios no se llaman entre sí para avisar de cosas que no necesitan respuesta inmediata:
publican un **evento** y quien le interese lo consume. El que publica no sabe quién escucha,
y si el consumidor está caído, el mensaje espera en la cola.

## Elementos en RabbitMQ

| Elemento | Nombre | Tipo |
|---|---|---|
| Exchange de eventos | `mstock.eventos` | topic, durable |
| Exchange de mensajes muertos | `mstock.eventos.dlx` | direct, durable |
| Cola de alertas de stock | `notificaciones.stock-bajo` | durable, enlazada con `stock.bajo` |
| Cola de ventas | `notificaciones.venta-registrada` | durable, enlazada con `venta.registrada` |
| Cola de mensajes muertos | `notificaciones.dlq` | durable |

Si un mensaje no se puede procesar, se reintenta 3 veces (1 s, 2 s) y después pasa a
`notificaciones.dlq`, donde se puede revisar sin perderlo.

## Contrato de los eventos

Siempre JSON, con `content_type: application/json`.

### `stock.bajo` (lo publica inventario-service)

Se publica al confirmarse una salida o un ajuste que deja el stock en o bajo el mínimo. `productoNombre` va `null` porque el nombre es del catálogo; el consumidor muestra el id.

    {
      "productoId": 15,
      "productoNombre": "Gaseosa 500 ml",
      "cantidad": 3,
      "stockMinimo": 5
    }

### `venta.registrada` (lo publica ventas-service)

Se publica al confirmarse la transacción que guarda la venta. `cantidadItems` es el número de productos distintos.

    {
      "ventaId": 120,
      "usuario": "cajero1",
      "total": 25.50,
      "cantidadItems": 3
    }

### `compra.registrada` (lo publica compras-service)

Se publica al confirmarse la compra (estado `PENDIENTE`). Inventario la consume y suma el stock.

    {
      "compraId": 7,
      "proveedorId": 1,
      "usuario": "almacenero",
      "items": [ { "productoId": 10, "productoNombre": "Arroz", "cantidad": 4, "costoUnitario": 2.50 } ]
    }

### `compra.aplicada` y `compra.rechazada` (las publica inventario-service)

Es la respuesta a `compra.registrada`; compras-service las consume en la cola `compras.resultado`.

- `compra.aplicada`: el stock ya se sumó (se envía tras el commit). La compra pasa a `RECIBIDA`. Si llega un `compra.registrada` repetido, se vuelve a enviar para que el aviso perdido no deje la compra pendiente.
- `compra.rechazada`: el evento es inválido (sin productos, cantidad inválida) o se agotaron los 3 reintentos. Nada se sumó (la transacción se revirtió). La compra pasa a `RECHAZADA` con `motivoRechazo`.

      { "compraId": 7, "motivo": null }

compras-service solo cambia una compra que sigue `PENDIENTE`, así que un resultado repetido o tardío se ignora. Sus mensajes fallidos van a `compras.dlq`.

## Reglas del consumidor

- Las alertas `stock.bajo` no se duplican: si el producto ya tiene una alerta sin leer, el evento se ignora.
- Un evento sin `productoId` o sin `ventaId` es inválido: termina en la cola de mensajes muertos.
- El consumidor convierte el JSON a su propia clase usando el tipo del método que escucha,
  por eso el publicador y el consumidor no comparten código.
