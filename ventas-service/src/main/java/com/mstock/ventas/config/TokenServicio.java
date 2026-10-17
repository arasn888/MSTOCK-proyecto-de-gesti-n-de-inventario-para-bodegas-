package com.mstock.ventas.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Cuenta de servicio de ventas-service: token de vida corta con rol SERVICIO, firmado con la misma
 * clave que el auth-service. Se usa solo para llamadas internas que un usuario (cajero) no debe poder
 * hacer por su cuenta, como devolver stock al compensar una venta que no se guardó.
 */
@Component
public class TokenServicio {

    private final JwtEncoder encoder;

    public TokenServicio(JwtProperties props) {
        SecretKey clave = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    public String generar() {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mstock-auth")
                .subject("ventas-service")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(2, ChronoUnit.MINUTES))
                .claim("rol", "SERVICIO")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
