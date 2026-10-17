package com.mstock.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * El gateway es la primera barrera: rechaza (401) toda petición sin un token válido antes de gastar
 * una llamada a los servicios. Los permisos por rol (403) los siguen decidiendo los servicios, que
 * también validan el mismo JWT (defensa en profundidad: saltarse el gateway no sirve de nada).
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Misma clave y mismo emisor ("mstock-auth") que usan los servicios. */
    @Bean
    ReactiveJwtDecoder jwtDecoder(JwtProperties props) {
        SecretKey clave = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("mstock-auth"));
        return decoder;
    }

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http, ErroresSeguridadHandler errores) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores));
        return http.build();
    }
}
