package com.mstock.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VentaRequest(
        @Positive(message = "El id del cliente debe ser positivo")
        Long clienteId,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        @Size(max = 100, message = "Una venta admite hasta 100 productos distintos")
        List<@Valid ItemVentaRequest> items) {
}
