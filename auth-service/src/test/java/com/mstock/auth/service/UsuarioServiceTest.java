package com.mstock.auth.service;

import com.mstock.auth.dto.UsuarioUpdateRequest;
import com.mstock.auth.entity.Rol;
import com.mstock.auth.entity.Usuario;
import com.mstock.auth.exception.OperacionNoPermitidaException;
import com.mstock.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UsuarioService service;

    private Usuario usuario(long id, String username, Rol rol) {
        return Usuario.builder().id(id).username(username).password("x").nombres("N").apellidos("A")
                .email(username + "@mstock.com").rol(rol).build();
    }

    private UsuarioUpdateRequest cambio(String username, Rol rol, boolean activo) {
        return new UsuarioUpdateRequest("N", "A", username + "@mstock.com", rol, activo, null);
    }

    @Test
    @DisplayName("Un admin no puede desactivarse a sí mismo")
    void noSeDesactivaASiMismo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1, "admin", Rol.ADMIN)));

        assertThatThrownBy(() -> service.actualizar(1L, cambio("admin", Rol.ADMIN, false), "admin"))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("Un admin no puede quitarse a sí mismo el rol de administrador")
    void noSeQuitaElRol() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1, "admin", Rol.ADMIN)));

        assertThatThrownBy(() -> service.actualizar(1L, cambio("admin", Rol.CAJERO, true), "admin"))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("Un admin sí puede desactivar a otro usuario")
    void desactivaAOtro() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "cajero1", Rol.CAJERO)));

        var respuesta = service.actualizar(2L, cambio("cajero1", Rol.CAJERO, false), "admin");

        assertThat(respuesta).isNotNull();
    }

    @Test
    @DisplayName("Un admin puede editarse a sí mismo sin perder acceso (cambiar nombres)")
    void seEditaSinPerderAcceso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1, "admin", Rol.ADMIN)));

        var respuesta = service.actualizar(1L, cambio("admin", Rol.ADMIN, true), "admin");

        assertThat(respuesta).isNotNull();
    }
}
