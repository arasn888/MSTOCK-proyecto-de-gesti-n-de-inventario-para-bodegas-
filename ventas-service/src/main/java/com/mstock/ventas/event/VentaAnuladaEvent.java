package com.mstock.ventas.event;

import java.math.BigDecimal;

/** Evento que consume notificaciones-service (routing key: venta.anulada). Ver docs/eventos-rabbitmq.md. */
public record VentaAnuladaEvent(
        Long ventaId,
        String numero,
        String usuario,
        BigDecimal total,
        String motivo) {
}
