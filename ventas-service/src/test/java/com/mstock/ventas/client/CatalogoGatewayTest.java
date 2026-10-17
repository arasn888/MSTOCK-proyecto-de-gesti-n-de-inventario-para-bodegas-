package com.mstock.ventas.client;

import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ServicioNoDisponibleException;
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

import java.math.BigDecimal;
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

    private CatalogoGateway gateway;

    @BeforeEach
    void preparar() {
        CircuitBreaker cb = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(10).minimumNumberOfCalls(10)
                .ignoreExceptions(RecursoNoEncontradoException.class).build());
        Retry retry = Retry.of("test", RetryConfig.custom().maxAttempts(3)
                .waitDuration(Duration.ofMillis(1)).retryExceptions(FalloCatalogoException.class).build());
        gateway = new CatalogoGateway(client, cb, retry);
    }

    private static FeignException error(int estado) {
        Request request = Request.create(Request.HttpMethod.GET, "http://catalogo-service/api/productos/1",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response respuesta = Response.builder().status(estado).reason("error").request(request)
                .headers(Map.of()).build();
        return FeignException.errorStatus("ProductoCatalogoClient#obtener", respuesta);
    }

    @Test
    @DisplayName("Devuelve el producto con su precio vigente y reenvía el token")
    void devuelveElProducto() {
        ProductoCatalogo p = new ProductoCatalogo(1L, "ARR-1", "Arroz", new BigDecimal("3.50"), true);
        when(client.obtener(1L, "Bearer abc")).thenReturn(p);

        assertThat(gateway.consultarProducto(1L, "abc")).isEqualTo(p);
    }

    @Test
    @DisplayName("404: producto inexistente, sin reintentos")
    void inexistente() {
        when(client.obtener(any(), any())).thenThrow(error(404));

        assertThatThrownBy(() -> gateway.consultarProducto(1L, "abc"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(client, times(1)).obtener(any(), any());
    }

    @Test
    @DisplayName("Catálogo caído: 3 intentos (es una lectura) y luego 503")
    void caidoReintenta() {
        when(client.obtener(any(), any())).thenThrow(error(503));

        assertThatThrownBy(() -> gateway.consultarProducto(1L, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class);
        verify(client, times(3)).obtener(any(), any());
    }
}
