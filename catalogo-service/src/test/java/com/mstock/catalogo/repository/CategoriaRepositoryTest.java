package com.mstock.catalogo.repository;

import com.mstock.catalogo.entity.Categoria;
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
        "spring.datasource.url=jdbc:h2:mem:catalogotest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository repository;

    private Categoria categoria(String nombre) {
        return Categoria.builder().nombre(nombre).descripcion("Descripción de " + nombre).build();
    }

    @Test
    @DisplayName("Insertar: guarda la categoría y le asigna id")
    void insertar() {
        Categoria guardada = repository.save(categoria("Bebidas"));

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Listar: devuelve todas las categorías")
    void listar() {
        repository.save(categoria("Bebidas"));
        repository.save(categoria("Lácteos"));

        assertThat(repository.findAll()).extracting(Categoria::getNombre)
                .containsExactlyInAnyOrder("Bebidas", "Lácteos");
    }

    @Test
    @DisplayName("Actualizar: conserva los cambios")
    void actualizar() {
        Categoria guardada = repository.save(categoria("Bebidas"));

        guardada.setNombre("Bebidas y jugos");
        guardada.setActivo(false);
        repository.saveAndFlush(guardada);

        Categoria recuperada = repository.findById(guardada.getId()).orElseThrow();
        assertThat(recuperada.getNombre()).isEqualTo("Bebidas y jugos");
        assertThat(recuperada.isActivo()).isFalse();
    }

    @Test
    @DisplayName("Eliminar: borra la categoría")
    void eliminar() {
        Categoria guardada = repository.save(categoria("Bebidas"));

        repository.deleteById(guardada.getId());
        repository.flush();

        assertThat(repository.findById(guardada.getId())).isEmpty();
    }

    @Test
    @DisplayName("Buscar por nombre ignora mayúsculas y minúsculas")
    void buscarPorNombre() {
        repository.save(categoria("Bebidas"));

        assertThat(repository.existsByNombreIgnoreCase("BEBIDAS")).isTrue();
        assertThat(repository.findByNombreIgnoreCase("bebidas")).isPresent();
        assertThat(repository.existsByNombreIgnoreCase("Ropa")).isFalse();
    }

    @Test
    @DisplayName("Nombre duplicado: la base de datos lo rechaza")
    void nombreDuplicado() {
        repository.saveAndFlush(categoria("Bebidas"));

        assertThatThrownBy(() -> repository.saveAndFlush(categoria("Bebidas")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}