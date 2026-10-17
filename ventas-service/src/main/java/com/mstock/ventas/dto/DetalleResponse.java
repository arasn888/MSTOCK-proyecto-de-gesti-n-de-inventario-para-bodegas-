package com.mstock.ventas.dto;

import com.mstock.ventas.entity.DetalleVenta;

import java.math.BigDecimal;

public record DetalleResponse(Long productoId, String codigo, String nombre, int cantidad,
                              BigDecimal precioUnitario, BigDecimal subtotal) {

    public static DetalleResponse desde(DetalleVenta d) {
        return new DetalleResponse(d.getProductoId(), d.getCodigo(), d.getNombre(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}
