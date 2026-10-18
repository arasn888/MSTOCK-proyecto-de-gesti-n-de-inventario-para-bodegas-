package com.mstock.ventas.client;

import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.exception.ResultadoIncierto;
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
    private static final String OP = "7f3c1a52-0d1e-4c8a-9a55-1b2c3d4e5f60";

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
    @DisplayName("Descuento correcto: envía los productos con el id de operación y el token del usuario")
    void descuentoCorrecto() {
        gateway.descontar(OP, ITEMS, "abc");

        verify(client).salidaLote(new LoteRequest(ITEMS, "Venta de mostrador", OP), "Bearer abc");
    }

    @Test
    @DisplayName("409 que no es de stock: no se disfraza de stock insuficiente")
    void conflictoQueNoEsStock() {
        doThrow(error(409, "{\"estado\":409,\"mensaje\":\"otra operación modificó el mismo registro\"}"))
                .when(client).salidaLote(any(), any());

        assertThatThrownBy(() -> gateway.descontar(OP, ITEMS, "abc"))
                .isInstanceOf(com.mstock.ventas.exception.ReglaNegocioException.class)
                .hasMessageContaining("otra operación");
    }

    @Test
    @DisplayName("409 del inventario: stock insuficiente con el mensaje original, sin contar como caída")
    void stockInsuficiente() {
        doThrow(error(409, "{\"estado\":409,\"mensaje\":\"Stock insuficiente: disponible 1, solicitado 5\"}"))
                .when(client).salidaLote(any(), any());

        assertThatThrownBy(() -> gateway.descontar(OP, ITEMS, "abc"))
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

        assertThatThrownBy(() -> gateway.descontar(OP, ITEMS, "abc"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("Timeout o 5xx: se repite UNA vez con el mismo id y, si sigue sin respuesta, el resultado es incierto")
    void inventarioCaidoSeReintentaUnaVez() {
        doThrow(error(503, "")).when(client).salidaLote(any(), any());

        assertThatThrownBy(() -> gateway.descontar(OP, ITEMS, "abc"))
                .isInstanceOf(ResultadoIncierto.class)
                .isInstanceOf(ServicioNoDisponibleException.class);
        org.mockito.ArgumentCaptor<LoteRequest> enviados = org.mockito.ArgumentCaptor.forClass(LoteRequest.class);
        verify(client, times(2)).salidaLote(enviados.capture(), any());
        // Los dos intentos llevan el mismo id: así el inventario no descuenta dos veces.
        assertThat(enviados.getAllValues()).extracting(LoteRequest::operacionId).containsOnly(OP);
    }

    @Test
    @DisplayName("Se perdió la respuesta del primer intento: el segundo funciona y la venta sigue adelante")
    void segundoIntentoFunciona() {
        org.mockito.Mockito.doThrow(error(504, "")).doNothing().when(client).salidaLote(any(), any());

        gateway.descontar(OP, ITEMS, "abc");

        verify(client, times(2)).salidaLote(new LoteRequest(ITEMS, "Venta de mostrador", OP), "Bearer abc");
    }

    @Test
    @DisplayName("Un error de negocio (stock insuficiente) no se reintenta")
    void errorDeNegocioNoSeReintenta() {
        doThrow(error(409, "{\"mensaje\":\"Stock insuficiente: disponible 1, solicitado 5\"}"))
                .when(client).salidaLote(any(), any());

        assertThatThrownBy(() -> gateway.descontar(OP, ITEMS, "abc")).isInstanceOf(StockInsuficienteException.class);
        verify(client, times(1)).salidaLote(any(), any());
    }

    @Test
    @DisplayName("Con el inventario caído el circuito se abre y deja de llamarlo")
    void elCircuitoSeAbre() {
        doThrow(error(503, "")).when(client).salidaLote(any(), any());

        // Cada descuento hace 2 intentos fallidos: con 2 descuentos se llenan las 4 llamadas de la ventana.
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> gateway.descontar(OP, ITEMS, "abc")).isInstanceOf(ResultadoIncierto.class);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> gateway.descontar(OP, ITEMS, "abc"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .isNotInstanceOf(ResultadoIncierto.class)
                .hasMessageContaining("no está respondiendo");
        verify(client, times(4)).salidaLote(any(), any());
    }

    @Test
    @DisplayName("Reversión por operación: usa su endpoint con el id en la ruta")
    void reversionDeOperacion() {
        gateway.revertirOperacion(OP, "Anulación de venta B001-1", "abc");

        verify(client).reversionDeOperacion(OP, new ReversionOperacionRequest("Anulación de venta B001-1"), "Bearer abc");
    }

    @Test
    @DisplayName("La reversión usa su propio endpoint")
    void reversion() {
        gateway.revertir(ITEMS, "abc");

        verify(client).reversion(new LoteRequest(ITEMS, "Reversión de venta no registrada"), "Bearer abc");
    }
}
