CREATE TABLE notificaciones (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    tipo          VARCHAR(20)  NOT NULL,
    titulo        VARCHAR(120) NOT NULL,
    mensaje       VARCHAR(500) NOT NULL,
    referencia_id BIGINT       NULL,
    leida         BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_notificaciones_leida_fecha (leida, fecha)
);
