package com.mstock.catalogo.repository;

import com.mstock.catalogo.entity.Categoria;
import com.mstock.catalogo.entity.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Búsqueda por nombre o código y por código exacto (lector de código de barras). */
@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:productobusquedatest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ProductoBusquedaRepositoryTest {

    @Autowired
    private ProductoRepository repository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private Categoria bebidas;
    private Categoria lacteos;

    @BeforeEach
    void preparar() {
        bebidas = categoriaRepository.save(Categoria.builder().nombre("Bebidas").build());
        lacteos = categoriaRepository.save(Categoria.builder().nombre("Lácteos").build());
        repository.save(producto("7750182001234", "Gaseosa 500 ml", bebidas, true));
        repository.save(producto("7750182009999", "Leche entera 1 L", lacteos, true));
        repository.save(producto("AGU-001", "Agua mineral 625 ml", bebidas, true));
        repository.save(producto("7750182005555", "Gaseosa retirada", bebidas, false));
    }

    private Producto producto(String codigo, String nombre, Categoria categoria, boolean activo) {
        return Producto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .precioCompra(new BigDecimal("2.50"))
                .precioVenta(new BigDecimal("3.50"))
                .categoria(categoria)
                .activo(activo)
                .build();
    }

    @Test
    @DisplayName("Código exacto: encuentra el producto activo con su categoría")
    void codigoExacto() {
        assertThat(repository.findByCodigoAndActivoTrue("7750182001234"))
                .hasValueSatisfying(p -> {
                    assertThat(p.getNombre()).isEqualTo("Gaseosa 500 ml");
                    assertThat(p.getCategoria().getNombre()).isEqualTo("Bebidas");
                });
    }

    @Test
    @DisplayName("Código exacto: un código parcial no coincide")
    void codigoParcialNoCoincide() {
        assertThat(repository.findByCodigoAndActivoTrue("7750182")).isEmpty();
    }

    @Test
    @DisplayName("Código exacto: un producto inactivo no se encuentra (no se puede vender)")
    void codigoDeProductoInactivo() {
        assertThat(repository.findByCodigoAndActivoTrue("7750182005555")).isEmpty();
    }

    @Test
    @DisplayName("Texto: busca por parte del nombre sin distinguir mayúsculas")
    void textoPorNombre() {
        assertThat(repository.buscarActivosPorTexto("gaseosa"))
                .extracting(Producto::getNombre).containsExactly("Gaseosa 500 ml");
    }

    @Test
    @DisplayName("Texto: también busca por parte del código")
    void textoPorCodigo() {
        assertThat(repository.buscarActivosPorTexto("agu-0"))
                .extracting(Producto::getNombre).containsExactly("Agua mineral 625 ml");
        assertThat(repository.buscarActivosPorTexto("7750182"))
                .extracting(Producto::getCodigo).containsExactlyInAnyOrder("7750182001234", "7750182009999");
    }

    @Test
    @DisplayName("Texto: los resultados salen ordenados por nombre y solo activos")
    void ordenadoYSoloActivos() {
        assertThat(repository.buscarActivosPorTexto("a"))
                .extracting(Producto::getNombre)
                .containsExactly("Agua mineral 625 ml", "Gaseosa 500 ml", "Leche entera 1 L");
    }

    @Test
    @DisplayName("Texto y categoría: filtra por las dos cosas")
    void textoYCategoria() {
        assertThat(repository.buscarActivosPorTextoYCategoria("7750182", bebidas.getId()))
                .extracting(Producto::getNombre).containsExactly("Gaseosa 500 ml");
        assertThat(repository.buscarActivosPorTextoYCategoria("7750182", lacteos.getId()))
                .extracting(Producto::getNombre).containsExactly("Leche entera 1 L");
    }
}
