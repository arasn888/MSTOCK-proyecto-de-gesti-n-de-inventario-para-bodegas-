package com.mstock.inventario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AjusteRequest(
        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El id del producto debe ser positivo")
        Long productoId,

        @NotNull(message = "La cantidad real es obligatoria")
        @Min(value = 0, message = "La cantidad real no puede ser negativa")
        @Max(value = 1000000, message = "La cantidad real no puede superar 1 000 000")
        Integer cantidadReal,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 200, message = "El motivo admite hasta 200 caracteres")
        String motivo) {
}
