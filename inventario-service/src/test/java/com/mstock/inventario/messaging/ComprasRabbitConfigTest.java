package com.mstock.inventario.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ComprasRabbitConfigTest {

    @Test
    @DisplayName("Saca el compraId del cuerpo JSON aunque el mensaje no se pueda convertir")
    void extraeCompraId() {
        Message m = new Message("{\"compraId\": 42, \"items\": []}".getBytes(StandardCharsets.UTF_8));
        assertThat(ComprasRabbitConfig.extraerCompraId(m)).isEqualTo(42L);
    }

    @Test
    @DisplayName("Mensaje sin compraId: devuelve null")
    void sinCompraId() {
        Message m = new Message("basura".getBytes(StandardCharsets.UTF_8));
        assertThat(ComprasRabbitConfig.extraerCompraId(m)).isNull();
    }
}
