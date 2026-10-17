package com.mstock.ventas.client;

import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.exception.ServicioNoDisponibleException;
import com.mstock.ventas.exception.StockInsuficienteException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventarioGatewayTest {

    private static final List<ItemLote> ITEMS = List.of(new ItemLote(1L, 2));

    @Mock
    private InventarioClient client;

    private CircuitBreaker circuitBreaker;
    private InventarioGateway gateway;

    @BeforeEach
    void preparar() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .ignoreExceptions(StockInsuficienteException.class, ReglaNegocioException.class,
                        RecursoNoEncontradoException.class)
                .build());
        gateway = new InventarioGateway(client, circuitBreaker);
    }

    private static FeignException error(int estado, String cuerpo) {
        Request request = Request.create(Request.HttpMethod.POST,
                "http://inventario-service/api/inventario/salidas/lote", Map.of(), null,
                StandardCharsets.UTF_8, null);
        Response respuesta = Response.builder().status(estado).reason("error").request(request)
                .headers(Map.of()).body(cuerpo, StandardCharsets.UTF_8).build();
        return FeignException.errorStatus("InventarioClient#salidaLote", respuesta);
    }

    @Test
    @DisplayName("Descuento correcto: envía los productos con el token del usuario")
    void descuentoCorrecto() {
        gateway.descontar(ITEMS, "abc");

        verify(client).salidaLote(new LoteRequest(ITEMS, "Venta de mostrador"), "Bearer abc");
    }

    @Test
    @DisplayName("409 del inventario: stock insuficiente con el mensaje original, sin contar como caída")
    void stockInsuficiente() {
        doThrow(error(409, "{\"estado\":409,\"mensaje\":\"Stock insuficiente: disponible 1, solicitado 5\"}"))
                .when(client).salidaLote(any(), any());

        assertThatThrownBy(() -> gateway.descontar(ITEMS, "abc"))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessage("Stock insuficiente: disponible 1, solicitado 5");
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("400 del inventario (producto inactivo): error de regla de negocio")
    void productoInactivo() {
        doThrow(error(400, "{\"mensaje\":\"El producto ARR-1 está inactivo\"}"))
                .when(client).salidaLote(any(), any());

        assertThatThrownBy(() -> gateway.descontar(ITEMS, "abc"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("Inventario caído: 503 y NO se reintenta (un POST repetido podría descontar dos veces)")
    void inventarioCaidoNoSeReintenta() {
        doThrow(error(503, "")).when(client).salidaLote(any(), any());

        assertThatThrownBy(() -> gateway.descontar(ITEMS, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class);
        verify(client, times(1)).salidaLote(any(), any());
    }

    @Test
    @DisplayName("Con el inventario caído el circuito se abre y deja de llamarlo")
    void elCircuitoSeAbre() {
        doThrow(error(503, "")).when(client).salidaLote(any(), any());

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> gateway.descontar(ITEMS, "abc"))
                    .isInstanceOf(ServicioNoDisponibleException.class);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> gateway.descontar(ITEMS, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("no está respondiendo");
        verify(client, times(4)).salidaLote(any(), any());
    }

    @Test
    @DisplayName("La reversión usa su propio endpoint")
    void reversion() {
        gateway.revertir(ITEMS, "abc");

        verify(client).reversion(new LoteRequest(ITEMS, "Reversión de venta no registrada"), "Bearer abc");
    }
}
