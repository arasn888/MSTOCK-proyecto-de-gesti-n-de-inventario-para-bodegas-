package com.mstock.auth.config;

import com.mstock.auth.entity.Rol;
import com.mstock.auth.entity.Usuario;
import com.mstock.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Variable de entorno ADMIN_PASSWORD (.env). Vacía = contraseña de desarrollo. */
    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByUsername("admin")) {
            return;
        }
        String clave = adminPassword;
        if (clave == null || clave.isBlank()) {
            clave = "Admin123*";
            log.warn("ADMIN_PASSWORD no está definida: se usa la contraseña de desarrollo. "
                    + "Define ADMIN_PASSWORD en .env antes de desplegar.");
        }
        usuarioRepository.save(Usuario.builder()
                .username("admin")
                .password(passwordEncoder.encode(clave))
                .nombres("Administrador")
                .apellidos("MSTOCK")
                .email("admin@mstock.com")
                .rol(Rol.ADMIN)
                .build());
        log.info("Usuario administrador inicial creado: admin");
    }
}
