package com.mstock.ventas.client;

import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.exception.ServicioNoDisponibleException;
import com.mstock.ventas.exception.StockInsuficienteException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descuento y reversión de stock. Son escrituras (POST): NO se reintentan, porque si el inventario
 * ya descontó y solo se perdió la respuesta, un reintento descontaría dos veces.
 * Solo hay circuit breaker, para dejar de insistir cuando el inventario está caído.
 */
@Slf4j
@Component
public class InventarioGateway {

    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"([^\"]*)\"");

    private final InventarioClient client;
    private final CircuitBreaker circuitBreaker;

    public InventarioGateway(InventarioClient client,
                             @Qualifier("inventarioCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
    }

    public void descontar(List<ItemLote> items, String token) {
        ejecutar(() -> client.salidaLote(new LoteRequest(items, "Venta de mostrador"), "Bearer " + token),
                "descontar el stock");
    }

    public void revertir(List<ItemLote> items, String token) {
        ejecutar(() -> client.reversion(new LoteRequest(items, "Reversión de venta no registrada"),
                "Bearer " + token), "revertir el stock");
    }

    private void ejecutar(Runnable operacion, String accion) {
        try {
            circuitBreaker.executeRunnable(() -> llamar(operacion));
        } catch (CallNotPermittedException e) {
            throw new ServicioNoDisponibleException(
                    "El inventario no está respondiendo: no se pudo " + accion + ". La venta no se registró. "
                            + "Inténtalo en unos segundos");
        } catch (FalloInventarioException e) {
            throw new ServicioNoDisponibleException("No se pudo " + accion + ": el inventario no respondió");
        }
    }

    private void llamar(Runnable operacion) {
        try {
            operacion.run();
        } catch (FeignException.Conflict e) {
            throw new StockInsuficienteException(mensaje(e, "Stock insuficiente"));
        } catch (FeignException.BadRequest e) {
            throw new ReglaNegocioException(mensaje(e, "El inventario rechazó la operación"));
        } catch (FeignException.NotFound e) {
            throw new RecursoNoEncontradoException(mensaje(e, "Producto no encontrado"));
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            throw new ServicioNoDisponibleException("El inventario rechazó las credenciales enviadas por ventas");
        } catch (FeignException e) {
            log.warn("Fallo al llamar al inventario (estado {}): {}", e.status(), e.getMessage());
            throw new FalloInventarioException("Inventario no disponible (estado " + e.status() + ")", e);
        }
    }

    static String mensaje(FeignException e, String porDefecto) {
        String cuerpo = e.contentUTF8();
        Matcher m = MENSAJE.matcher(cuerpo == null ? "" : cuerpo);
        return m.find() ? m.group(1) : porDefecto;
    }
}
