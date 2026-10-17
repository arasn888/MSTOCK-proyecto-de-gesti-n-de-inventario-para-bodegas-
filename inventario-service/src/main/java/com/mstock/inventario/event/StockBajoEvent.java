package com.mstock.inventario.event;

/** Evento que consume notificaciones-service (routing key: stock.bajo). Ver docs/eventos-rabbitmq.md. */
public record StockBajoEvent(
        Long productoId,
        String productoNombre,
        Integer cantidad,
        Integer stockMinimo) {
}
