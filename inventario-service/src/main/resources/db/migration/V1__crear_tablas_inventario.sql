-- Existencias actuales: una fila por producto.
-- producto_id apunta al catálogo, pero NO es clave foránea: son bases de datos distintas.
CREATE TABLE stock (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    producto_id         BIGINT      NOT NULL,
    cantidad            INT         NOT NULL DEFAULT 0,
    stock_minimo        INT         NOT NULL DEFAULT 5,
    fecha_actualizacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_producto UNIQUE (producto_id),
    CONSTRAINT ck_stock_cantidad CHECK (cantidad >= 0),
    CONSTRAINT ck_stock_minimo CHECK (stock_minimo >= 0)
);

-- Kardex: historial inmutable de entradas, salidas y ajustes.
CREATE TABLE movimientos (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    producto_id      BIGINT       NOT NULL,
    tipo             VARCHAR(10)  NOT NULL,
    cantidad         INT          NOT NULL,
    stock_anterior   INT          NOT NULL,
    stock_resultante INT          NOT NULL,
    motivo           VARCHAR(200) NULL,
    usuario          VARCHAR(50)  NOT NULL,
    fecha            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_movimientos_producto_fecha (producto_id, fecha)
);
