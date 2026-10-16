package com.mstock.auth.repository;

import com.mstock.auth.entity.Rol;
import com.mstock.auth.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:authtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    private Usuario nuevoUsuario(String username, String email) {
        return Usuario.builder()
                .username(username)
                .password("$2a$10$hashDePruebaNoEsUnaClaveReal")
                .nombres("Juan")
                .apellidos("Pérez")
                .email(email)
                .rol(Rol.CAJERO)
                .build();
    }

    @Test
    @DisplayName("Insertar: guarda el usuario, asigna id y fecha de creación")
    void insertar() {
        Usuario guardado = repository.save(nuevoUsuario("jperez", "jperez@mstock.com"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(guardado.isActivo()).isTrue();
        assertThat(repository.findById(guardado.getId())).isPresent();
    }

    @Test
    @DisplayName("Listar: devuelve todos los usuarios guardados")
    void listar() {
        repository.save(nuevoUsuario("usuario1", "u1@mstock.com"));
        repository.save(nuevoUsuario("usuario2", "u2@mstock.com"));

        List<Usuario> usuarios = repository.findAll();

        assertThat(usuarios).hasSize(2);
        assertThat(usuarios).extracting(Usuario::getUsername)
                .containsExactlyInAnyOrder("usuario1", "usuario2");
    }

    @Test
    @DisplayName("Actualizar: modifica los datos y los conserva")
    void actualizar() {
        Usuario guardado = repository.save(nuevoUsuario("jperez", "jperez@mstock.com"));

        guardado.setNombres("Carlos");
        guardado.setRol(Rol.ALMACENERO);
        repository.saveAndFlush(guardado);

        Usuario recuperado = repository.findById(guardado.getId()).orElseThrow();
        assertThat(recuperado.getNombres()).isEqualTo("Carlos");
        assertThat(recuperado.getRol()).isEqualTo(Rol.ALMACENERO);
    }

    @Test
    @DisplayName("Eliminar: borra el usuario de la base de datos")
    void eliminar() {
        Usuario guardado = repository.save(nuevoUsuario("jperez", "jperez@mstock.com"));

        repository.deleteById(guardado.getId());
        repository.flush();

        assertThat(repository.findById(guardado.getId())).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Buscar por username: encuentra al usuario o devuelve vacío")
    void buscarPorUsername() {
        repository.save(nuevoUsuario("jperez", "jperez@mstock.com"));

        Optional<Usuario> existente = repository.findByUsername("jperez");
        Optional<Usuario> inexistente = repository.findByUsername("fantasma");

        assertThat(existente).isPresent();
        assertThat(existente.get().getEmail()).isEqualTo("jperez@mstock.com");
        assertThat(inexistente).isEmpty();
        assertThat(repository.existsByUsername("jperez")).isTrue();
        assertThat(repository.existsByEmail("otro@mstock.com")).isFalse();
    }

    @Test
    @DisplayName("Username duplicado: la base de datos lo rechaza")
    void usernameDuplicado() {
        repository.saveAndFlush(nuevoUsuario("jperez", "uno@mstock.com"));

        assertThatThrownBy(() ->
                repository.saveAndFlush(nuevoUsuario("jperez", "dos@mstock.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}