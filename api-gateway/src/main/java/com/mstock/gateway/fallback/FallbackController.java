package com.mstock.gateway.fallback;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Respuesta de respaldo cuando el circuit breaker de una ruta se activa (servicio caído, sin
 * instancias o demasiado lento). Atiende cualquier método HTTP porque la ruta reenvía el original.
 */
@RestController
public class FallbackController {

    private static final Map<String, String> NOMBRES = Map.of(
            "auth", "autenticación (auth-service)",
            "catalogo", "catálogo (catalogo-service)",
            "inventario", "inventario (inventario-service)",
            "ventas", "ventas (ventas-service)",
            "notificaciones", "notificaciones (notificaciones-service)");

    @RequestMapping("/fallback/{servicio}")
    public ResponseEntity<ErrorResponse> respaldo(@PathVariable String servicio, ServerHttpRequest request) {
        return construir(servicio, HttpMethod.GET.equals(request.getMethod()));
    }

    /** Respaldo para una lectura: no hubo ningún cambio, así que se puede decir que no se realizó nada. */
    public ResponseEntity<ErrorResponse> respaldo(String servicio) {
        return construir(servicio, true);
    }

    private ResponseEntity<ErrorResponse> construir(String servicio, boolean lectura) {
        String nombre = NOMBRES.getOrDefault(servicio, servicio);
        // En una escritura (venta, entrada...) el gateway puede cortar por tiempo cuando el servicio
        // ya la había completado: decir "no se realizó" llevaría al usuario a repetirla y duplicarla.
        String detalle = lectura
                ? "La operación no se realizó. Inténtalo en unos segundos"
                : "No se pudo confirmar si la operación se realizó: verifica antes de reintentarla "
                        + "(por ejemplo, consulta el listado de ventas de hoy)";
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                503,
                "El servicio de " + nombre + " no está respondiendo. " + detalle,
                Instant.now()));
    }
}
