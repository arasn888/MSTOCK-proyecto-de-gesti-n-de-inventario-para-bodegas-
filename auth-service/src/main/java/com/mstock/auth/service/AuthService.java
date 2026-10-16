package com.mstock.auth.service;

import com.mstock.auth.config.JwtProperties;
import com.mstock.auth.dto.LoginRequest;
import com.mstock.auth.dto.LoginResponse;
import com.mstock.auth.entity.Usuario;
import com.mstock.auth.exception.CredencialesInvalidasException;
import com.mstock.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(CredencialesInvalidasException::new);

        Instant ahora = Instant.now();
        Instant expira = ahora.plus(jwtProperties.expiracionMinutos(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mstock-auth")
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("rol", usuario.getRol().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(
                token,
                "Bearer",
                jwtProperties.expiracionMinutos() * 60,
                usuario.getUsername(),
                usuario.getRol().name());
    }
}