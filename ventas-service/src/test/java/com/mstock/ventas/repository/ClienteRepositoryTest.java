package com.mstock.ventas.repository;

import com.mstock.ventas.entity.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:clientetest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    private Cliente cliente(String documento, String nombres) {
        return Cliente.builder().documento(documento).nombres(nombres).build();
    }

    @Test
    @DisplayName("Insertar: guarda el cliente activo y con fecha de creación")
    void insertar() {
        Cliente guardado = repository.save(cliente("12345678", "Ana Torres"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getFechaCreacion()).isNotNull();
    }

    @Test
    @DisplayName("Actualizar: cambia el nombre y el teléfono")
    void actualizar() {
        Cliente guardado = repository.saveAndFlush(cliente("12345678", "Ana Torres"));
        guardado.setNombres("Ana Torres Díaz");
        guardado.setTelefono("987654321");
        repository.saveAndFlush(guardado);

        Cliente leido = repository.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getNombres()).isEqualTo("Ana Torres Díaz");
        assertThat(leido.getTelefono()).isEqualTo("987654321");
    }

    @Test
    @DisplayName("Eliminar: el cliente deja de existir")
    void eliminar() {
        Cliente guardado = repository.saveAndFlush(cliente("12345678", "Ana Torres"));

        repository.deleteById(guardado.getId());

        assertThat(repository.findById(guardado.getId())).isEmpty();
    }

    @Test
    @DisplayName("Listar: solo activos, ordenados por nombre, con búsqueda parcial sin distinguir mayúsculas")
    void listarYBuscar() {
        Cliente inactivo = cliente("33333333", "Carlos Inactivo");
        inactivo.setActivo(false);
        repository.save(cliente("11111111", "Beto Ramos"));
        repository.save(cliente("22222222", "Ana Ramos"));
        repository.save(inactivo);

        assertThat(repository.findByActivoTrueOrderByNombresAsc())
                .extracting(Cliente::getNombres).containsExactly("Ana Ramos", "Beto Ramos");
        assertThat(repository.findByActivoTrueAndNombresContainingIgnoreCaseOrderByNombresAsc("ramos"))
                .hasSize(2);
    }

    @Test
    @DisplayName("Documento duplicado: la base de datos lo rechaza; existsBy lo detecta")
    void documentoDuplicado() {
        repository.saveAndFlush(cliente("12345678", "Ana Torres"));

        assertThat(repository.existsByDocumento("12345678")).isTrue();
        assertThatThrownBy(() -> repository.saveAndFlush(cliente("12345678", "Otra Persona")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Varios clientes sin documento son válidos")
    void variosSinDocumento() {
        repository.saveAndFlush(cliente(null, "Cliente uno"));
        repository.saveAndFlush(cliente(null, "Cliente dos"));

        assertThat(repository.count()).isEqualTo(2);
    }
}
