package com.mstock.gateway.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El gateway debe aceptar exactamente los tokens que emite el auth-service, y ninguno más. */
class JwtDecoderTest {

    private static final String SECRETO = "clave-de-prueba-de-al-menos-treinta-y-dos-caracteres-1234";

    private ReactiveJwtDecoder decoder;

    @BeforeEach
    void preparar() {
        decoder = new SecurityConfig().jwtDecoder(new JwtProperties(SECRETO));
    }

    private String token(String secreto, String emisor, Instant expira) {
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject("cajero")
                .issuedAt(expira.minus(1, ChronoUnit.HOURS))
                .expiresAt(expira)
                .claim("rol", "CAJERO")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    @DisplayName("Token válido: lo acepta y conserva usuario y rol")
    void tokenValido() {
        Jwt jwt = decoder.decode(token(SECRETO, "mstock-auth", Instant.now().plus(1, ChronoUnit.HOURS))).block();

        assertThat(jwt).isNotNull();
        assertThat(jwt.getSubject()).isEqualTo("cajero");
        assertThat(jwt.<String>getClaim("rol")).isEqualTo("CAJERO");
    }

    @Test
    @DisplayName("Token firmado con otra clave: lo rechaza")
    void otraClave() {
        String falso = token("otra-clave-distinta-de-al-menos-treinta-y-dos-caracteres-99", "mstock-auth",
                Instant.now().plus(1, ChronoUnit.HOURS));

        assertThatThrownBy(() -> decoder.decode(falso).block()).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token de otro emisor: lo rechaza")
    void otroEmisor() {
        String ajeno = token(SECRETO, "otro-emisor", Instant.now().plus(1, ChronoUnit.HOURS));

        assertThatThrownBy(() -> decoder.decode(ajeno).block()).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token expirado: lo rechaza")
    void expirado() {
        String viejo = token(SECRETO, "mstock-auth", Instant.now().minus(10, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> decoder.decode(viejo).block()).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Texto que no es un JWT: lo rechaza")
    void basura() {
        assertThatThrownBy(() -> decoder.decode("esto-no-es-un-token").block()).isInstanceOf(JwtException.class);
    }
}
