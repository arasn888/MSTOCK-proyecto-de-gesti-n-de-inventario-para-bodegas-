-- Clientes (opcionales en una venta de mostrador).
CREATE TABLE clientes (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    documento      VARCHAR(15)  NULL,
    nombres        VARCHAR(120) NOT NULL,
    telefono       VARCHAR(20)  NULL,
    email          VARCHAR(120) NULL,
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_creacion DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_clientes_documento UNIQUE (documento)
);

-- Cabecera de la venta. cliente_id sí es clave foránea: está en la misma base de datos.
CREATE TABLE ventas (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    numero     VARCHAR(20)    NULL,
    cliente_id BIGINT         NULL,
    usuario    VARCHAR(50)    NOT NULL,
    fecha      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total      DECIMAL(12, 2) NOT NULL,
    estado     VARCHAR(15)    NOT NULL DEFAULT 'COMPLETADA',
    PRIMARY KEY (id),
    CONSTRAINT uk_ventas_numero UNIQUE (numero),
    CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_ventas_total CHECK (total >= 0),
    INDEX idx_ventas_fecha (fecha)
);

-- Detalle: guarda código, nombre y precio del momento, porque el catálogo puede cambiar después.
-- producto_id apunta al catálogo pero NO es clave foránea (otra base de datos).
CREATE TABLE detalle_venta (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    venta_id        BIGINT         NOT NULL,
    producto_id     BIGINT         NOT NULL,
    codigo          VARCHAR(30)    NOT NULL,
    nombre          VARCHAR(120)   NOT NULL,
    cantidad        INT            NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    subtotal        DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    INDEX idx_detalle_venta (venta_id)
);
