package com.mstock.inventario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockMinimoRequest(
        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        @Max(value = 1000000, message = "El stock mínimo no puede superar 1 000 000")
        Integer stockMinimo) {
}
