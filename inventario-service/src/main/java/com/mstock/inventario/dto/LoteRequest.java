package com.mstock.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Varios productos en una sola operación: se aplican todos o ninguno.
 * {@code operacionId} es opcional: si viene, la operación es idempotente (el mismo id enviado dos
 * veces se aplica una sola vez y la segunda respuesta devuelve los movimientos de la primera).
 */
public record LoteRequest(
        @NotEmpty(message = "Debe incluir al menos un producto")
        @Size(max = 100, message = "Un lote admite hasta 100 productos")
        List<@Valid ItemLote> items,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 200, message = "El motivo admite hasta 200 caracteres")
        String motivo,

        @Pattern(regexp = "^[A-Za-z0-9-]{8,60}$",
                message = "El id de operación debe tener entre 8 y 60 letras, números o guiones")
        String operacionId) {

    public LoteRequest(List<ItemLote> items, String motivo) {
        this(items, motivo, null);
    }
}
