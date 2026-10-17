package com.mstock.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ErrorResponse> respaldo(@PathVariable String servicio) {
        String nombre = NOMBRES.getOrDefault(servicio, servicio);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                503,
                "El servicio de " + nombre + " no está respondiendo. La operación no se realizó. "
                        + "Inténtalo en unos segundos",
                Instant.now()));
    }
}
