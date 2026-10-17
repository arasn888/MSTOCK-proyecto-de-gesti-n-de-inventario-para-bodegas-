package com.mstock.inventario.client;

import com.mstock.inventario.exception.RecursoNoEncontradoException;
import com.mstock.inventario.exception.ServicioNoDisponibleException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoGatewayTest {

    @Mock
    private ProductoCatalogoClient client;

    private CircuitBreaker circuitBreaker;
    private CatalogoGateway gateway;

    @BeforeEach
    void preparar() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(3).minimumNumberOfCalls(3).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .ignoreExceptions(RecursoNoEncontradoException.class).build());
        Retry retry = Retry.of("test", RetryConfig.custom().maxAttempts(3)
                .waitDuration(Duration.ofMillis(1)).retryExceptions(FalloCatalogoException.class).build());
        gateway = new CatalogoGateway(client, circuitBreaker, retry);
    }

    private static FeignException error(int estado) {
        Request request = Request.create(Request.HttpMethod.GET, "http://catalogo-service/api/productos/1",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response respuesta = Response.builder().status(estado).reason("error").request(request)
                .headers(Map.of()).build();
        return FeignException.errorStatus("ProductoCatalogoClient#obtener", respuesta);
    }

    @Test
    @DisplayName("Devuelve el producto y reenvía el token del usuario")
    void devuelveElProducto() {
        ProductoCatalogo p = new ProductoCatalogo(1L, "ARR-1", "Arroz", true);
        when(client.obtener(1L, "Bearer abc")).thenReturn(p);

        assertThat(gateway.consultarProducto(1L, "abc")).isEqualTo(p);
    }

    @Test
    @DisplayName("404: el producto no existe en el catálogo, sin reintentos")
    void inexistente() {
        when(client.obtener(any(), any())).thenThrow(error(404));

        assertThatThrownBy(() -> gateway.consultarProducto(1L, "abc"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(client, times(1)).obtener(any(), any());
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    @DisplayName("401 del catálogo: 503 con mensaje de credenciales, sin reintentos")
    void credencialesRechazadas() {
        when(client.obtener(any(), any())).thenThrow(error(401));

        assertThatThrownBy(() -> gateway.consultarProducto(1L, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("credenciales");
        verify(client, times(1)).obtener(any(), any());
    }

    @Test
    @DisplayName("Catálogo caído: 3 intentos (es una lectura) y luego 503")
    void caidoReintenta() {
        when(client.obtener(any(), any())).thenThrow(error(503));

        assertThatThrownBy(() -> gateway.consultarProducto(1L, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("No se pudo consultar el catálogo");
        verify(client, times(3)).obtener(any(), any());
    }

    @Test
    @DisplayName("Con el catálogo caído el circuito se abre y deja de llamarlo")
    void elCircuitoSeAbre() {
        when(client.obtener(any(), any())).thenThrow(error(503));

        // Los 3 intentos del reintento llenan la ventana (3 llamadas) y abren el circuito
        assertThatThrownBy(() -> gateway.consultarProducto(1L, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> gateway.consultarProducto(1L, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("no está respondiendo");
        verify(client, times(3)).obtener(any(), any());
    }
}
