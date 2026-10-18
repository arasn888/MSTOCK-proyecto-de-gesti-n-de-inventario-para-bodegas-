-- operacion_id: id con el que se pidió el descuento de stock al inventario. Sirve para deshacer
-- exactamente esa operación al anular la venta (o si se perdió la respuesta). Null en las ventas
-- anteriores a este cambio.
ALTER TABLE ventas
    ADD COLUMN operacion_id      VARCHAR(64)  NULL,
    ADD COLUMN motivo_anulacion  VARCHAR(200) NULL,
    ADD COLUMN fecha_anulacion   DATETIME(6)  NULL,
    ADD COLUMN usuario_anulacion VARCHAR(50)  NULL,
    ADD CONSTRAINT uk_ventas_operacion UNIQUE (operacion_id);
