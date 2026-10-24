package com.mstock.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * Deja que Prometheus lea las metricas sin token. Solo aplica a /actuator/prometheus; las rutas de la API
 * siguen protegidas por la cadena principal. Ninguna ruta del gateway apunta a /actuator, asi que no se
 * publica hacia afuera.
 */
@Configuration
public class MetricasSecurityConfig {

    @Bean
    @Order(1)
    SecurityWebFilterChain metricasFilterChain(ServerHttpSecurity http) {
        return http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/prometheus"))
                .authorizeExchange(auth -> auth.anyExchange().permitAll())
                .build();
    }
}
