package com.mstock.gateway.config;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    /**
     * CORS para que Angular (otro origen, p. ej. http://localhost:4200) pueda llamar a la API desde el navegador.
     * Se resuelve aquí, dentro de la cadena de seguridad y antes de la autenticación, porque el preflight
     * (OPTIONS) que manda el navegador nunca lleva token: si lo evaluara primero la autenticación, recibiría 401.
     * Solo se permiten los orígenes listados (nunca "*"). La autenticación va en la cabecera Authorization, no
     * en cookies, así que no se permiten credenciales. Los servicios internos no necesitan CORS: solo el
     * gateway recibe tráfico del navegador.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${mstock.cors.allowed-origins:http://localhost:4200}") List<String> origenes) {
        List<String> permitidos = origenes.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (permitidos.isEmpty()) {
            throw new IllegalStateException("mstock.cors.allowed-origins no puede estar vacío");
        }
        if (permitidos.contains("*")) {
            throw new IllegalStateException(
                    "mstock.cors.allowed-origins no admite \"*\": lista los orígenes permitidos uno por uno");
        }
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(permitidos);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Sin esto el navegador no deja leer el nombre de archivo de las descargas (boleta, kardex) ni el Location.
        cors.setExposedHeaders(List.of("Content-Disposition", "Location"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http, ErroresSeguridadHandler errores,
                                       CorsConfigurationSource cors) {
        http
                .cors(c -> c.configurationSource(cors))
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
