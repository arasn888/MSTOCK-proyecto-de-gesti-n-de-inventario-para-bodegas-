package com.mstock.notificaciones.event;

/** Evento que publica el inventario-service (routing key: stock.bajo). */
public record StockBajoEvent(
        Long productoId,
        String productoNombre,
        Integer cantidad,
        Integer stockMinimo) {
}
