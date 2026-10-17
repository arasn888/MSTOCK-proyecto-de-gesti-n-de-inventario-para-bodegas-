package com.mstock.inventario.dto;

import com.mstock.inventario.entity.Stock;

import java.time.LocalDateTime;

public record StockResponse(
        Long id,
        Long productoId,
        int cantidad,
        int stockMinimo,
        boolean bajoMinimo,
        LocalDateTime fechaActualizacion) {

    public static StockResponse desde(Stock s) {
        return new StockResponse(
                s.getId(),
                s.getProductoId(),
                s.getCantidad(),
                s.getStockMinimo(),
                s.bajoMinimo(),
                s.getFechaActualizacion());
    }
}
