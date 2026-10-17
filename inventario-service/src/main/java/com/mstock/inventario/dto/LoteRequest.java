package com.mstock.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Varios productos en una sola operación: se aplican todos o ninguno. */
public record LoteRequest(
        @NotEmpty(message = "Debe incluir al menos un producto")
        @Size(max = 100, message = "Un lote admite hasta 100 productos")
        List<@Valid ItemLote> items,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 200, message = "El motivo admite hasta 200 caracteres")
        String motivo) {
}
