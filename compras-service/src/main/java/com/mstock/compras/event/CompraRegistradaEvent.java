package com.mstock.compras.event;

import java.math.BigDecimal;
import java.util.List;

public record CompraRegistradaEvent(Long compraId, Long proveedorId, String usuario,
                                    List<ItemCompraEvent> items) {
    public record ItemCompraEvent(Long productoId, String productoNombre, Integer cantidad,
                                  BigDecimal costoUnitario) {
    }
}
