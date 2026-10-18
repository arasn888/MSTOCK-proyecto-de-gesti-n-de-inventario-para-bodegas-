package com.mstock.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El navegador solo deja que Angular llame a la API si el gateway responde bien al preflight (OPTIONS). */
class CorsTest {

    private static final String ANGULAR = "http://localhost:4200";

    private CorsConfigurationSource fuente;

    @BeforeEach
    void preparar() {
        fuente = new SecurityConfig().corsConfigurationSource(List.of(ANGULAR, " http://127.0.0.1:4200 "));
    }

    private CorsConfiguration paraRuta(String ruta) {
        return fuente.getCorsConfiguration(MockServerWebExchange.from(MockServerHttpRequest.get(ruta).build()));
    }

    @Test
    @DisplayName("Permite el origen de Angular, los métodos de la API y las cabeceras Authorization y Content-Type")
    void permiteAngular() {
        CorsConfiguration cors = paraRuta("/api/ventas/1/boleta");

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin(ANGULAR)).isEqualTo(ANGULAR);
        assertThat(cors.checkOrigin("http://127.0.0.1:4200")).isEqualTo("http://127.0.0.1:4200");
        for (HttpMethod metodo : List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
                HttpMethod.DELETE, HttpMethod.OPTIONS)) {
            assertThat(cors.checkHttpMethod(metodo)).as(metodo.name()).isNotNull();
        }
        assertThat(cors.checkHeaders(List.of("authorization", "content-type"))).isNotNull();
    }

    @Test
    @DisplayName("Rechaza cualquier otro origen, método o cabecera")
    void rechazaLoDemas() {
        CorsConfiguration cors = paraRuta("/api/productos");

        assertThat(cors.checkOrigin("http://malo.example")).isNull();
        assertThat(cors.checkOrigin("http://localhost:4201")).isNull();
        assertThat(cors.checkOrigin("https://localhost:4200")).isNull();
        assertThat(cors.checkHttpMethod(HttpMethod.PATCH)).isNull();
        assertThat(cors.checkHeaders(List.of("x-secreto"))).isNull();
    }

    @Test
    @DisplayName("Expone Content-Disposition (nombre de las descargas) y Location, sin credenciales")
    void exponeCabeceras() {
        CorsConfiguration cors = paraRuta("/api/inventario/1/kardex/csv");

        assertThat(cors.getExposedHeaders()).contains("Content-Disposition", "Location");
        assertThat(cors.getAllowCredentials()).isFalse();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Solo aplica a /api: las demás rutas (actuator, fallback) no tienen CORS")
    void soloApi() {
        assertThat(paraRuta("/actuator/health")).isNull();
        assertThat(paraRuta("/fallback/ventas")).isNull();
    }

    @Test
    @DisplayName("Falla al arrancar si los orígenes están vacíos o piden \"*\"")
    void configuracionInvalida() {
        SecurityConfig config = new SecurityConfig();

        assertThatThrownBy(() -> config.corsConfigurationSource(List.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("vacío");
        assertThatThrownBy(() -> config.corsConfigurationSource(List.of(" ", "")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("vacío");
        assertThatThrownBy(() -> config.corsConfigurationSource(List.of(ANGULAR, "*")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("*");
    }
}
