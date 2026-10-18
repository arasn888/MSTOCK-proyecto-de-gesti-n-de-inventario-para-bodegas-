-- Idempotencia de operaciones de stock. Cada operación remota (p. ej. el descuento de una venta)
-- trae un id único; si el mismo id llega otra vez (reintento tras un timeout) no se vuelve a aplicar.
-- La reversión de una operación se guarda con el id "REV:<id>", que además sirve de marca para
-- rechazar un descuento tardío de una operación que ya fue cancelada.
CREATE TABLE operaciones (
    operacion_id VARCHAR(64) NOT NULL,
    registrada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (operacion_id)
);

ALTER TABLE movimientos
    ADD COLUMN operacion_id VARCHAR(64) NULL,
    ADD INDEX idx_movimientos_operacion (operacion_id);
