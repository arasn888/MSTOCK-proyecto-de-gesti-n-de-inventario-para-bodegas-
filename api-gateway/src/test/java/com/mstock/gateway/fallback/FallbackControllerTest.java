package com.mstock.gateway.fallback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

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
    @DisplayName("En una escritura no afirma que la operación no se realizó: pide verificar antes de reintentar")
    void escrituraPideVerificar() {
        var peticion = MockServerHttpRequest.post("/fallback/ventas").build();

        String mensaje = controller.respaldo("ventas", peticion).getBody().mensaje();

        assertThat(mensaje).contains("no está respondiendo").contains("verifica antes de reintentarla");
        assertThat(mensaje).doesNotContain("no se realizó");
    }

    @Test
    @DisplayName("En una lectura sí dice que no se realizó nada")
    void lecturaDiceNoSeRealizo() {
        var peticion = MockServerHttpRequest.get("/fallback/ventas").build();

        assertThat(controller.respaldo("ventas", peticion).getBody().mensaje()).contains("no se realizó");
    }

    @Test
    @DisplayName("Un servicio desconocido igual responde 503")
    void desconocido() {
        assertThat(controller.respaldo("otro").getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
