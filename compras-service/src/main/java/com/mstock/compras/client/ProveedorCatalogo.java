package com.mstock.compras.client;

public record ProveedorCatalogo(Long id, String ruc, String razonSocial, String telefono,
                                String email, String direccion, boolean activo) {
}
