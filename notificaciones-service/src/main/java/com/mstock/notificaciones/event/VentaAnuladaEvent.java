package com.mstock.notificaciones.event;

import java.math.BigDecimal;

/** Evento que publica el ventas-service (routing key: venta.anulada). */
public record VentaAnuladaEvent(
        Long ventaId,
        String numero,
        String usuario,
        BigDecimal total,
        String motivo) {
}
