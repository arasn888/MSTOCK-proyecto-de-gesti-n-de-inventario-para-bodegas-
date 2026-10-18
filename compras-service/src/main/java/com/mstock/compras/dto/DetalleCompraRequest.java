package com.mstock.compras.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleCompraRequest(
        @NotNull @Positive Long productoId,
        @NotNull @Positive @Max(1000000) Integer cantidad) {
}
