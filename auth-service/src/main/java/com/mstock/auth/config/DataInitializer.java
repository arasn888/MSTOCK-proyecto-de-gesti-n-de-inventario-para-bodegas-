package com.mstock.auth.config;

import com.mstock.auth.entity.Rol;
import com.mstock.auth.entity.Usuario;
import com.mstock.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByUsername("admin")) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin123*"))
                .nombres("Administrador")
                .apellidos("MSTOCK")
                .email("admin@mstock.com")
                .rol(Rol.ADMIN)
                .build());
        log.info("Usuario administrador inicial creado: admin");
    }
}
