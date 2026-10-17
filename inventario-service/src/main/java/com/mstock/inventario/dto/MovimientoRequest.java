package com.mstock.inventario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MovimientoRequest(
        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El id del producto debe ser positivo")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 1000000, message = "La cantidad no puede superar 1 000 000")
        Integer cantidad,

        @Size(max = 200, message = "El motivo admite hasta 200 caracteres")
        String motivo) {
}
