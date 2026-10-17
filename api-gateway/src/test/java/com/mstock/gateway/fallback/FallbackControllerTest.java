package com.mstock.gateway.fallback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    @DisplayName("Responde 503 y nombra el servicio caído")
    void nombraElServicio() {
        ResponseEntity<ErrorResponse> respuesta = controller.respaldo("ventas");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().estado()).isEqualTo(503);
        assertThat(respuesta.getBody().mensaje()).contains("ventas").contains("no está respondiendo");
    }

    @Test
    @DisplayName("Cada servicio tiene su propio mensaje")
    void mensajePorServicio() {
        assertThat(controller.respaldo("inventario").getBody().mensaje()).contains("inventario");
        assertThat(controller.respaldo("catalogo").getBody().mensaje()).contains("catálogo");
        assertThat(controller.respaldo("auth").getBody().mensaje()).contains("autenticación");
    }

    @Test
    @DisplayName("Un servicio desconocido igual responde 503")
    void desconocido() {
        assertThat(controller.respaldo("otro").getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
