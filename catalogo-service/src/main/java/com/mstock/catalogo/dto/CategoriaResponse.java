package com.mstock.catalogo.dto;

import com.mstock.catalogo.entity.Categoria;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activo) {

    public static CategoriaResponse desde(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.isActivo());
    }
}