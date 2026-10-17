package com.mstock.catalogo.dto;

import com.mstock.catalogo.entity.Proveedor;

public record ProveedorResponse(
        Long id,
        String ruc,
        String razonSocial,
        String telefono,
        String email,
        String direccion,
        boolean activo) {

    public static ProveedorResponse desde(Proveedor p) {
        return new ProveedorResponse(
                p.getId(),
                p.getRuc(),
                p.getRazonSocial(),
                p.getTelefono(),
                p.getEmail(),
                p.getDireccion(),
                p.isActivo());
    }
}
