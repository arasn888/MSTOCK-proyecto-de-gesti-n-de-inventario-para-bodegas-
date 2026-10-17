package com.mstock.ventas.client;

import java.math.BigDecimal;

/** Lo que la venta necesita del catálogo: identificación y el precio vigente. */
public record ProductoCatalogo(Long id, String codigo, String nombre, BigDecimal precioVenta, boolean activo) {
}
