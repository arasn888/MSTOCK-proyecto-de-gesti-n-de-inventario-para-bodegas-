package com.mstock.notificaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Deja que Prometheus lea las metricas sin token. Solo aplica a /actuator/prometheus: el resto de la API
 * sigue protegido por la cadena principal. El gateway no enruta /actuator, asi que no se publica hacia afuera;
 * dentro de la red de Docker o del cluster solo lo alcanzan los demas contenedores.
 */
@Configuration
public class MetricasSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain metricasFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/prometheus")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
