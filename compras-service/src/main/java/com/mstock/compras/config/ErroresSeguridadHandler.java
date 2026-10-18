package com.mstock.compras.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Responde en JSON, con el mismo formato que el resto de errores de la API,
 * cuando falta el token (401), el token no es válido (401) o el rol no alcanza (403).
 */
@Component
public class ErroresSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean enviaToken = request.getHeader("Authorization") != null;
        escribir(response, 401, enviaToken
                ? "Token inválido o expirado"
                : "Se requiere autenticación: envía un token Bearer");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, 403, "No tienes permiso para realizar esta operación");
    }

    private void escribir(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"estado\":" + estado
                + ",\"mensaje\":\"" + mensaje
                + "\",\"fecha\":\"" + Instant.now() + "\"}");
    }
}
