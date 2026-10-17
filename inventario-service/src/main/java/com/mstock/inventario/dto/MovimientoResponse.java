package com.mstock.inventario.dto;

import com.mstock.inventario.entity.Movimiento;
import com.mstock.inventario.entity.TipoMovimiento;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        Long productoId,
        TipoMovimiento tipo,
        int cantidad,
        int stockAnterior,
        int stockResultante,
        String motivo,
        String usuario,
        LocalDateTime fecha) {

    public static MovimientoResponse desde(Movimiento m) {
        return new MovimientoResponse(
                m.getId(),
                m.getProductoId(),
                m.getTipo(),
                m.getCantidad(),
                m.getStockAnterior(),
                m.getStockResultante(),
                m.getMotivo(),
                m.getUsuario(),
                m.getFecha());
    }
}
