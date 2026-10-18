package com.mstock.ventas.client;

import java.math.BigDecimal;

/**
 * Lo que la venta necesita del catálogo: identificación, precio vigente y costo (para calcular ganancias).
 * El costo nunca se muestra al cajero: solo se guarda en la línea de venta.
 */
public record ProductoCatalogo(Long id, String codigo, String nombre, BigDecimal precioVenta,
                               BigDecimal precioCompra, boolean activo) {

    public ProductoCatalogo(Long id, String codigo, String nombre, BigDecimal precioVenta, boolean activo) {
        this(id, codigo, nombre, precioVenta, null, activo);
    }
}
