package com.mstock.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReversionOperacionRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 190, message = "El motivo admite hasta 190 caracteres")
        String motivo) {
}
