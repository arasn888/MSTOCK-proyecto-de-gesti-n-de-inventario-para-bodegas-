-- Estados: PENDIENTE (nueva), RECIBIDA (inventario sumó el stock), RECHAZADA (inventario no pudo).
-- Las compras que ya existían se registraron cuando el stock se sumaba sin confirmación: quedan RECIBIDA.
ALTER TABLE compras MODIFY COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE compras ADD COLUMN motivo_rechazo VARCHAR(255) NULL;
UPDATE compras SET estado = 'RECIBIDA' WHERE estado = 'REGISTRADA';
