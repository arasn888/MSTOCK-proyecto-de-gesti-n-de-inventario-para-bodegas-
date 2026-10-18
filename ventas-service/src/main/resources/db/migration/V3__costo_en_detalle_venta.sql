-- Costo del producto al momento de vender (para calcular ganancias sin consultar el catálogo después).
-- Null en las ventas anteriores a este cambio: sus líneas no entran en el cálculo de ganancia.
ALTER TABLE detalle_venta
    ADD COLUMN costo_unitario DECIMAL(10, 2) NULL;
