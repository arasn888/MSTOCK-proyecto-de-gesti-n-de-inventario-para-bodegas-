package com.mstock.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CompraRequest(
        @NotNull @Positive Long proveedorId,
        @NotEmpty List<@Valid DetalleCompraRequest> items) {
}
