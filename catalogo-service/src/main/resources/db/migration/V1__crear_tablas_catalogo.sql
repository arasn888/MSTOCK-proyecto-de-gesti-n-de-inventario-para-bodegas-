CREATE TABLE categorias (
                            id          BIGINT       NOT NULL AUTO_INCREMENT,
                            nombre      VARCHAR(80)  NOT NULL,
                            descripcion VARCHAR(255) NULL,
                            activo      BOOLEAN      NOT NULL DEFAULT TRUE,
                            PRIMARY KEY (id),
                            CONSTRAINT uk_categorias_nombre UNIQUE (nombre)
);

CREATE TABLE proveedores (
                             id           BIGINT       NOT NULL AUTO_INCREMENT,
                             ruc          VARCHAR(11)  NOT NULL,
                             razon_social VARCHAR(150) NOT NULL,
                             telefono     VARCHAR(20)  NULL,
                             email        VARCHAR(120) NULL,
                             direccion    VARCHAR(200) NULL,
                             activo       BOOLEAN      NOT NULL DEFAULT TRUE,
                             PRIMARY KEY (id),
                             CONSTRAINT uk_proveedores_ruc UNIQUE (ruc)
);

CREATE TABLE productos (
                           id                  BIGINT        NOT NULL AUTO_INCREMENT,
                           codigo              VARCHAR(30)   NOT NULL,
                           nombre              VARCHAR(120)  NOT NULL,
                           descripcion         VARCHAR(255)  NULL,
                           precio_compra       DECIMAL(10,2) NOT NULL,
                           precio_venta        DECIMAL(10,2) NOT NULL,
                           unidad_medida       VARCHAR(20)   NOT NULL DEFAULT 'UNIDAD',
                           categoria_id        BIGINT        NOT NULL,
                           proveedor_id        BIGINT        NULL,
                           activo              BOOLEAN       NOT NULL DEFAULT TRUE,
                           fecha_creacion      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           fecha_actualizacion DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           PRIMARY KEY (id),
                           CONSTRAINT uk_productos_codigo UNIQUE (codigo),
                           CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
                           CONSTRAINT fk_productos_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
                           INDEX idx_productos_nombre (nombre)
);