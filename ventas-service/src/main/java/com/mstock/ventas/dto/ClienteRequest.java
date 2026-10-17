package com.mstock.ventas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @Pattern(regexp = "^(\\d{8}|\\d{11})?$", message = "El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC)")
        String documento,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
        String nombres,

        @Size(max = 20, message = "El teléfono admite hasta 20 caracteres")
        String telefono,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo admite hasta 120 caracteres")
        String email) {
}
