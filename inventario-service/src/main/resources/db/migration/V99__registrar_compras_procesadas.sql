-- Idempotencia: evita aplicar dos veces la misma compra si RabbitMQ redelivery ocurre.
CREATE TABLE compras_procesadas (
    compra_id BIGINT NOT NULL,
    procesada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (compra_id)
);
