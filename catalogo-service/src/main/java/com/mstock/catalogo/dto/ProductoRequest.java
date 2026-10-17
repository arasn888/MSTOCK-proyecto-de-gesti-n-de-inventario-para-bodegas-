package com.mstock.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código admite hasta 30 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "El código solo admite letras, números, punto, guion y guion bajo")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción admite hasta 255 caracteres")
        String descripcion,

        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
        @Digits(integer = 8, fraction = 2,
                message = "El precio de compra admite hasta 8 enteros y 2 decimales")
        BigDecimal precioCompra,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a cero")
        @Digits(integer = 8, fraction = 2,
                message = "El precio de venta admite hasta 8 enteros y 2 decimales")
        BigDecimal precioVenta,

        @Size(max = 20, message = "La unidad de medida admite hasta 20 caracteres")
        String unidadMedida,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        Long proveedorId,

        Boolean activo) {
}
