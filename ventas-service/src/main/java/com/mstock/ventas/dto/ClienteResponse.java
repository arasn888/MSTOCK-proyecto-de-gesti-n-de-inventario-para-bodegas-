package com.mstock.ventas.dto;

import com.mstock.ventas.entity.Cliente;

public record ClienteResponse(Long id, String documento, String nombres, String telefono,
                              String email, boolean activo) {

    public static ClienteResponse desde(Cliente c) {
        return new ClienteResponse(c.getId(), c.getDocumento(), c.getNombres(),
                c.getTelefono(), c.getEmail(), c.isActivo());
    }
}
