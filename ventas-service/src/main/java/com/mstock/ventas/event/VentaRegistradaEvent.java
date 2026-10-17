package com.mstock.ventas.event;

import java.math.BigDecimal;

/** Evento que consume notificaciones-service (routing key: venta.registrada). Ver docs/eventos-rabbitmq.md. */
public record VentaRegistradaEvent(
        Long ventaId,
        String usuario,
        BigDecimal total,
        Integer cantidadItems) {
}
