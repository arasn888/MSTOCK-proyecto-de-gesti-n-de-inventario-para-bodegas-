package com.mstock.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnulacionRequest(
        @NotBlank(message = "El motivo de la anulación es obligatorio")
        @Size(max = 200, message = "El motivo admite hasta 200 caracteres")
        String motivo) {
}
