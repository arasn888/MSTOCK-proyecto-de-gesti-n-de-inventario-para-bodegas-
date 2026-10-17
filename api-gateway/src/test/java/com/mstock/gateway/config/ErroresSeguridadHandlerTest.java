package com.mstock.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class ErroresSeguridadHandlerTest {

    private final ErroresSeguridadHandler handler = new ErroresSeguridadHandler();

    @Test
    @DisplayName("Sin token: 401 pidiendo autenticación")
    void sinToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/ventas"));

        handler.commence(exchange, new BadCredentialsException("x")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"estado\":401").contains("Se requiere autenticación");
    }

    @Test
    @DisplayName("Con token malo: 401 diciendo que es inválido o expiró")
    void tokenInvalido() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/ventas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer basura"));

        handler.commence(exchange, new BadCredentialsException("x")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("Token inválido o expirado");
    }

    @Test
    @DisplayName("Acceso denegado: 403")
    void accesoDenegado() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/otra"));

        handler.handle(exchange, new AccessDeniedException("x")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"estado\":403");
    }
}
