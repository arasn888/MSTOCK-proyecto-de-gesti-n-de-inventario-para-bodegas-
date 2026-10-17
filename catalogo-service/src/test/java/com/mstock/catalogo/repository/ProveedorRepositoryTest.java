package com.mstock.catalogo.repository;

import com.mstock.catalogo.entity.Proveedor;
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
        "spring.datasource.url=jdbc:h2:mem:proveedortest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ProveedorRepositoryTest {

    @Autowired
    private ProveedorRepository repository;

    private Proveedor proveedor(String ruc, String razonSocial) {
        return Proveedor.builder().ruc(ruc).razonSocial(razonSocial).telefono("999888777").build();
    }

    @Test
    @DisplayName("Insertar: guarda el proveedor y le asigna id")
    void insertar() {
        Proveedor guardado = repository.save(proveedor("20123456789", "Distribuidora Andina SAC"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Listar: devuelve todos los proveedores")
    void listar() {
        repository.save(proveedor("20123456789", "Distribuidora Andina SAC"));
        repository.save(proveedor("20987654321", "Lácteos del Sur SRL"));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Actualizar: conserva los cambios")
    void actualizar() {
        Proveedor guardado = repository.save(proveedor("20123456789", "Distribuidora Andina SAC"));

        guardado.setRazonSocial("Andina Distribuciones SAC");
        guardado.setTelefono("944555666");
        repository.saveAndFlush(guardado);

        Proveedor recuperado = repository.findById(guardado.getId()).orElseThrow();
        assertThat(recuperado.getRazonSocial()).isEqualTo("Andina Distribuciones SAC");
        assertThat(recuperado.getTelefono()).isEqualTo("944555666");
    }

    @Test
    @DisplayName("Eliminar: borra el proveedor")
    void eliminar() {
        Proveedor guardado = repository.save(proveedor("20123456789", "Distribuidora Andina SAC"));

        repository.deleteById(guardado.getId());
        repository.flush();

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Buscar por RUC")
    void buscarPorRuc() {
        repository.save(proveedor("20123456789", "Distribuidora Andina SAC"));

        assertThat(repository.findByRuc("20123456789")).isPresent();
        assertThat(repository.findByRuc("20000000000")).isEmpty();
        assertThat(repository.existsByRuc("20123456789")).isTrue();
    }

    @Test
    @DisplayName("RUC duplicado: la base de datos lo rechaza")
    void rucDuplicado() {
        repository.saveAndFlush(proveedor("20123456789", "Distribuidora Andina SAC"));

        assertThatThrownBy(() -> repository.saveAndFlush(proveedor("20123456789", "Otra empresa SAC")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}