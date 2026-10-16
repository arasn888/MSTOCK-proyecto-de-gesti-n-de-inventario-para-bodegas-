package com.mstock.auth.service;

import com.mstock.auth.dto.UsuarioRequest;
import com.mstock.auth.dto.UsuarioResponse;
import com.mstock.auth.dto.UsuarioUpdateRequest;
import com.mstock.auth.entity.Usuario;
import com.mstock.auth.exception.OperacionNoPermitidaException;
import com.mstock.auth.exception.RecursoDuplicadoException;
import com.mstock.auth.exception.RecursoNoEncontradoException;
import com.mstock.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un usuario con el nombre '" + request.username() + "'");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un usuario con el correo '" + request.email() + "'");
        }

        Usuario guardado = usuarioRepository.save(Usuario.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .email(request.email())
                .rol(request.rol())
                .build());
        return UsuarioResponse.desde(guardado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscar(id);

        boolean cambioEmail = !usuario.getEmail().equalsIgnoreCase(request.email());
        if (cambioEmail && usuarioRepository.existsByEmail(request.email())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un usuario con el correo '" + request.email() + "'");
        }

        usuario.setNombres(request.nombres());
        usuario.setApellidos(request.apellidos());
        usuario.setEmail(request.email());
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }
        return UsuarioResponse.desde(usuario);
    }

    @Transactional
    public void eliminar(Long id, String usernameActual) {
        Usuario usuario = buscar(id);
        if (usuario.getUsername().equals(usernameActual)) {
            throw new OperacionNoPermitidaException("No puedes eliminar tu propio usuario");
        }
        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + id));
    }
}