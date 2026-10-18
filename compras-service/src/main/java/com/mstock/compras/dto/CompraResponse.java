package com.mstock.compras.dto;

import com.mstock.compras.entity.Compra;
import com.mstock.compras.entity.EstadoCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponse(Long id, Long proveedorId, String usuario, LocalDateTime fecha,
                             BigDecimal total, EstadoCompra estado, String motivoRechazo, List<DetalleCompraResponse> items) {
    public static CompraResponse desde(Compra c) {
        return new CompraResponse(c.getId(), c.getProveedorId(), c.getUsuario(), c.getFecha(),
                c.getTotal(), c.getEstado(), c.getMotivoRechazo(),
                c.getDetalles().stream().map(DetalleCompraResponse::desde).toList());
    }
}
