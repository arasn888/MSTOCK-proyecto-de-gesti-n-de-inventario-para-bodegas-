package com.mstock.gateway.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Responde en JSON, con el mismo formato que el resto de errores de la API, cuando falta el token
 * (401), el token no es válido o expiró (401) o el acceso se niega (403).
 */
@Component
public class ErroresSeguridadHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        boolean enviaToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION) != null;
        return escribir(exchange, 401, enviaToken
                ? "Token inválido o expirado"
                : "Se requiere autenticación: envía un token Bearer");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return escribir(exchange, 403, "No tienes permiso para realizar esta operación");
    }

    static String cuerpo(int estado, String mensaje) {
        return "{\"estado\":" + estado + ",\"mensaje\":\"" + mensaje + "\",\"fecha\":\"" + Instant.now() + "\"}";
    }

    private Mono<Void> escribir(ServerWebExchange exchange, int estado, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(estado));
        response.getHeaders().setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        byte[] bytes = cuerpo(estado, mensaje).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
