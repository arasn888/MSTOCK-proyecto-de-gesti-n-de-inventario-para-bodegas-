package com.mstock.inventario.event;

/** Respuesta del inventario a compra.registrada. {@code motivo} solo va cuando se rechaza. */
public record CompraResultadoEvent(Long compraId, String motivo) {
}
