package com.mstock.compras.dto;

import com.mstock.compras.entity.DetalleCompra;

import java.math.BigDecimal;

public record DetalleCompraResponse(Long id, Long productoId, String productoNombre,
                                    Integer cantidad, BigDecimal costoUnitario, BigDecimal subtotal) {
    public static DetalleCompraResponse desde(DetalleCompra d) {
        return new DetalleCompraResponse(d.getId(), d.getProductoId(), d.getProductoNombre(),
                d.getCantidad(), d.getCostoUnitario(), d.getSubtotal());
    }
}
