package com.mstock.auth.dto;

import com.mstock.auth.entity.Rol;
import com.mstock.auth.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String username,
        String nombres,
        String apellidos,
        String email,
        Rol rol,
        boolean activo,
        LocalDateTime fechaCreacion) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                u.getNombres(),
                u.getApellidos(),
                u.getEmail(),
                u.getRol(),
                u.isActivo(),
                u.getFechaCreacion());
    }
}