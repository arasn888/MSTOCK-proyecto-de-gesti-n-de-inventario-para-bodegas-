package com.mstock.notificaciones.event;

import java.math.BigDecimal;

/** Evento que publica el ventas-service (routing key: venta.registrada). */
public record VentaRegistradaEvent(
        Long ventaId,
        String usuario,
        BigDecimal total,
        Integer cantidadItems) {
}
