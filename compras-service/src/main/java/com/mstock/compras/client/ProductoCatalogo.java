package com.mstock.compras.client;

import java.math.BigDecimal;

public record ProductoCatalogo(Long id, String codigo, String nombre, String descripcion,
                               BigDecimal precioCompra, BigDecimal precioVenta,
                               String unidadMedida, Long categoriaId, String categoriaNombre,
                               Long proveedorId, String proveedorNombre, boolean activo) {
}
