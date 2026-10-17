package com.mstock.catalogo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "^(10|15|16|17|20)\\d{9}$",
                message = "El RUC debe tener 11 dígitos y empezar con 10, 15, 16, 17 o 20")
        String ruc,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social admite hasta 150 caracteres")
        String razonSocial,

        @Pattern(regexp = "^$|^[0-9+() -]{6,20}$", message = "El teléfono no tiene un formato válido")
        String telefono,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo admite hasta 120 caracteres")
        String email,

        @Size(max = 200, message = "La dirección admite hasta 200 caracteres")
        String direccion,

        Boolean activo) {
}
