package com.mstock.auth.dto;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String username,
        String rol) {
}