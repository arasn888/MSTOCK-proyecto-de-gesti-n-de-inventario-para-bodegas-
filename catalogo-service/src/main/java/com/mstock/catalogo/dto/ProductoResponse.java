package com.mstock.catalogo.dto;

import com.mstock.catalogo.entity.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        String unidadMedida,
        Long categoriaId,
        String categoriaNombre,
        Long proveedorId,
        String proveedorNombre,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public static ProductoResponse desde(Producto p) {
        boolean conProveedor = p.getProveedor() != null;
        return new ProductoResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecioCompra(),
                p.getPrecioVenta(),
                p.getUnidadMedida(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                conProveedor ? p.getProveedor().getId() : null,
                conProveedor ? p.getProveedor().getRazonSocial() : null,
                p.isActivo(),
                p.getFechaCreacion(),
                p.getFechaActualizacion());
    }
}
