package com.mstock.ventas.client;

import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.exception.ResultadoIncierto;
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
 * Descuento y reversión de stock. Son escrituras (POST), pero todas llevan un id de operación y el inventario
 * las trata como idempotentes: si ya aplicó la operación y solo se perdió la respuesta, repetirla con el mismo
 * id devuelve el resultado de la primera vez sin descontar otra vez. Por eso aquí SÍ se reintenta una vez
 * (solo ante timeout o 5xx). Además hay circuit breaker, para dejar de insistir cuando el inventario está caído.
 */
@Slf4j
@Component
public class InventarioGateway {

    static final int INTENTOS = 2;

    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"([^\"]*)\"");

    private final InventarioClient client;
    private final CircuitBreaker circuitBreaker;

    public InventarioGateway(InventarioClient client,
                             @Qualifier("inventarioCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Descuenta todos los productos o ninguno. Lanza {@link ResultadoIncierto} si tras reintentar no se supo
     * si el inventario aplicó el descuento.
     */
    public void descontar(String operacionId, List<ItemLote> items, String token) {
        ejecutar(() -> client.salidaLote(new LoteRequest(items, "Venta de mostrador", operacionId), "Bearer " + token),
                "descontar el stock", true);
    }

    /** Deshace lo que descontó la operación. No hace nada si la operación nunca se aplicó. */
    public void revertirOperacion(String operacionId, String motivo, String token) {
        ejecutar(() -> client.reversionDeOperacion(operacionId, new ReversionOperacionRequest(motivo),
                "Bearer " + token), "revertir el stock", true);
    }

    /** Devuelve stock por lista de productos. Con operacionId es idempotente; sin él, no. */
    public void revertir(List<ItemLote> items, String motivo, String operacionId, String token) {
        ejecutar(() -> client.reversion(new LoteRequest(items, motivo, operacionId), "Bearer " + token),
                "revertir el stock", operacionId != null);
    }

    public void revertir(List<ItemLote> items, String token) {
        revertir(items, "Reversión de venta no registrada", null, token);
    }

    private void ejecutar(Runnable operacion, String accion, boolean reintentable) {
        int intentos = reintentable ? INTENTOS : 1;
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                circuitBreaker.executeRunnable(() -> llamar(operacion));
                return;
            } catch (CallNotPermittedException e) {
                throw new ServicioNoDisponibleException(
                        "El inventario no está respondiendo: no se pudo " + accion + ". La operación no se registró. "
                                + "Inténtalo en unos segundos");
            } catch (FalloInventarioException e) {
                if (intento < intentos) {
                    log.warn("Sin respuesta del inventario al {} (intento {} de {}); se repite con el mismo id",
                            accion, intento, intentos);
                    continue;
                }
                throw new ResultadoIncierto("No se pudo " + accion + ": el inventario no respondió");
            }
        }
    }

    private void llamar(Runnable operacion) {
        try {
            operacion.run();
        } catch (FeignException.Conflict e) {
            String mensaje = mensaje(e, "Stock insuficiente");
            if (mensaje.startsWith("Stock insuficiente")) {
                throw new StockInsuficienteException(mensaje);
            }
            // Otro conflicto (p. ej. otra operación modificó el mismo registro): no es falta de stock.
            throw new ReglaNegocioException(mensaje);
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
