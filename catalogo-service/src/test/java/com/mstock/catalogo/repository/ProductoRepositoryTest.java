package com.mstock.catalogo.repository;

import com.mstock.catalogo.entity.Categoria;
import com.mstock.catalogo.entity.Producto;
import com.mstock.catalogo.entity.Proveedor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:productotest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository repository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Categoria bebidas;
    private Categoria lacteos;
    private Proveedor proveedor;

    @BeforeEach
    void preparar() {
        bebidas = categoriaRepository.save(Categoria.builder().nombre("Bebidas").build());
        lacteos = categoriaRepository.save(Categoria.builder().nombre("Lácteos").build());
        proveedor = proveedorRepository.save(
                Proveedor.builder().ruc("20123456789").razonSocial("Distribuidora Andina SAC").build());
    }

    private Producto producto(String codigo, String nombre, Categoria categoria, Proveedor prov) {
        return Producto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .precioCompra(new BigDecimal("2.50"))
                .precioVenta(new BigDecimal("3.50"))
                .categoria(categoria)
                .proveedor(prov)
                .build();
    }

    @Test
    @DisplayName("Insertar: guarda el producto con su categoría y proveedor")
    void insertar() {
        Producto guardado = repository.save(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(guardado.getUnidadMedida()).isEqualTo("UNIDAD");
        assertThat(guardado.getCategoria().getNombre()).isEqualTo("Bebidas");
        assertThat(guardado.getProveedor().getRuc()).isEqualTo("20123456789");
    }

    @Test
    @DisplayName("Insertar sin proveedor: el proveedor es opcional")
    void insertarSinProveedor() {
        Producto guardado = repository.save(producto("AGU-001", "Agua 625 ml", bebidas, null));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getProveedor()).isNull();
    }

    @Test
    @DisplayName("Listar: devuelve todos los productos")
    void listar() {
        repository.save(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));
        repository.save(producto("LEC-001", "Leche entera 1 L", lacteos, proveedor));

        List<Producto> productos = repository.findAll();

        assertThat(productos).extracting(Producto::getCodigo)
                .containsExactlyInAnyOrder("GAS-001", "LEC-001");
    }

    @Test
    @DisplayName("Actualizar: conserva el nuevo precio y la categoría")
    void actualizar() {
        Producto guardado = repository.save(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));

        guardado.setPrecioVenta(new BigDecimal("4.00"));
        guardado.setCategoria(lacteos);
        repository.saveAndFlush(guardado);
        entityManager.clear();

        Producto recuperado = repository.findById(guardado.getId()).orElseThrow();
        assertThat(recuperado.getPrecioVenta()).isEqualByComparingTo("4.00");
        assertThat(recuperado.getCategoria().getNombre()).isEqualTo("Lácteos");
    }

    @Test
    @DisplayName("Eliminar: borra el producto sin borrar su categoría")
    void eliminar() {
        Producto guardado = repository.save(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));

        repository.deleteById(guardado.getId());
        repository.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).isEmpty();
        assertThat(categoriaRepository.findById(bebidas.getId())).isPresent();
    }

    @Test
    @DisplayName("Buscar por nombre: ignora mayúsculas y encuentra coincidencias parciales")
    void buscarPorNombre() {
        repository.save(producto("GAS-001", "Gaseosa Cola 500 ml", bebidas, proveedor));
        repository.save(producto("GAS-002", "Gaseosa Naranja 500 ml", bebidas, proveedor));
        repository.save(producto("LEC-001", "Leche entera 1 L", lacteos, proveedor));

        assertThat(repository.findByNombreContainingIgnoreCase("gaseosa")).hasSize(2);
        assertThat(repository.findByNombreContainingIgnoreCase("ZUMO")).isEmpty();
    }

    @Test
    @DisplayName("Buscar por código")
    void buscarPorCodigo() {
        repository.save(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));

        assertThat(repository.findByCodigo("GAS-001")).isPresent();
        assertThat(repository.findByCodigo("XXX-000")).isEmpty();
        assertThat(repository.existsByCodigo("GAS-001")).isTrue();
    }

    @Test
    @DisplayName("Buscar por categoría: devuelve solo los productos de esa categoría")
    void buscarPorCategoria() {
        repository.save(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));
        repository.save(producto("LEC-001", "Leche entera 1 L", lacteos, proveedor));

        assertThat(repository.findByCategoriaId(bebidas.getId()))
                .extracting(Producto::getCodigo)
                .containsExactly("GAS-001");
    }

    @Test
    @DisplayName("Código duplicado: la base de datos lo rechaza")
    void codigoDuplicado() {
        repository.saveAndFlush(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));

        assertThatThrownBy(() ->
                repository.saveAndFlush(producto("GAS-001", "Otra gaseosa", bebidas, proveedor)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Contar productos por categoría y por proveedor")
    void contar() {
        repository.save(producto("GAS-001", "Gaseosa 500 ml", bebidas, proveedor));
        repository.save(producto("GAS-002", "Gaseosa 1 L", bebidas, proveedor));
        repository.save(producto("LEC-001", "Leche entera 1 L", lacteos, null));

        assertThat(repository.countByCategoriaId(bebidas.getId())).isEqualTo(2);
        assertThat(repository.countByCategoriaId(lacteos.getId())).isEqualTo(1);
        assertThat(repository.countByProveedorId(proveedor.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("Listar activos: excluye las bajas y filtra por categoría y nombre")
    void listarActivos() {
        repository.save(producto("GAS-001", "Gaseosa Cola 500 ml", bebidas, proveedor));
        Producto dadoDeBaja = producto("GAS-002", "Gaseosa Naranja 500 ml", bebidas, proveedor);
        dadoDeBaja.setActivo(false);
        repository.save(dadoDeBaja);
        repository.save(producto("LEC-001", "Leche entera 1 L", lacteos, proveedor));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByActivoTrue())
                .extracting(Producto::getCodigo)
                .containsExactlyInAnyOrder("GAS-001", "LEC-001");
        assertThat(repository.findByActivoTrueAndCategoriaId(bebidas.getId()))
                .extracting(Producto::getCodigo)
                .containsExactly("GAS-001");
        assertThat(repository.findByActivoTrueAndNombreContainingIgnoreCase("gaseosa")).hasSize(1);
        assertThat(repository.findByActivoTrueAndCategoriaIdAndNombreContainingIgnoreCase(
                bebidas.getId(), "gaseosa")).hasSize(1);
    }
}
