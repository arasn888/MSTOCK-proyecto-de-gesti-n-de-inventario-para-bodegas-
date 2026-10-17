package com.mstock.ventas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** El cliente solo indica qué producto y cuántas unidades: el precio lo pone el catálogo. */
public record ItemVentaRequest(
        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El id del producto debe ser positivo")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad por producto no puede superar 10 000")
        Integer cantidad) {
}
