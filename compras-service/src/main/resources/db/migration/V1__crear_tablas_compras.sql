CREATE TABLE compras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    proveedor_id BIGINT NOT NULL,
    usuario VARCHAR(50) NOT NULL,
    fecha DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    PRIMARY KEY (id),
    INDEX idx_compras_fecha (fecha),
    INDEX idx_compras_proveedor (proveedor_id)
);

CREATE TABLE detalle_compras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    compra_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    producto_nombre VARCHAR(160) NOT NULL,
    cantidad INT NOT NULL,
    costo_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_compras_compra
        FOREIGN KEY (compra_id) REFERENCES compras(id) ON DELETE CASCADE,
    CONSTRAINT ck_detalle_compras_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_compras_costo CHECK (costo_unitario >= 0),
    INDEX idx_detalle_compras_producto (producto_id)
);
