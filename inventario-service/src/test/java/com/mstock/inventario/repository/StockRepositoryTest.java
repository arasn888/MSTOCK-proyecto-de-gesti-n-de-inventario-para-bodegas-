package com.mstock.inventario.repository;

import com.mstock.inventario.entity.Stock;
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
        "spring.datasource.url=jdbc:h2:mem:stocktest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class StockRepositoryTest {

    @Autowired
    private StockRepository repository;

    private Stock stock(long productoId, int cantidad, int minimo) {
        return Stock.builder().productoId(productoId).cantidad(cantidad).stockMinimo(minimo).build();
    }

    @Test
    @DisplayName("Insertar: guarda el stock con valores por defecto y fecha")
    void insertar() {
        Stock guardado = repository.save(Stock.builder().productoId(10L).build());

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCantidad()).isZero();
        assertThat(guardado.getStockMinimo()).isEqualTo(5);
        assertThat(guardado.getFechaActualizacion()).isNotNull();
    }

    @Test
    @DisplayName("Listar: devuelve el stock ordenado por producto")
    void listar() {
        repository.save(stock(30L, 1, 5));
        repository.save(stock(10L, 1, 5));
        repository.save(stock(20L, 1, 5));

        assertThat(repository.findAllByOrderByProductoIdAsc())
                .extracting(Stock::getProductoId)
                .containsExactly(10L, 20L, 30L);
    }

    @Test
    @DisplayName("Actualizar: conserva la nueva cantidad y el nuevo mínimo")
    void actualizar() {
        Stock guardado = repository.save(stock(10L, 5, 5));

        guardado.setCantidad(50);
        guardado.setStockMinimo(8);
        repository.saveAndFlush(guardado);

        Stock recuperado = repository.findById(guardado.getId()).orElseThrow();
        assertThat(recuperado.getCantidad()).isEqualTo(50);
        assertThat(recuperado.getStockMinimo()).isEqualTo(8);
    }

    @Test
    @DisplayName("Eliminar: borra el registro de stock")
    void eliminar() {
        Stock guardado = repository.save(stock(10L, 5, 5));

        repository.deleteById(guardado.getId());
        repository.flush();

        assertThat(repository.findById(guardado.getId())).isEmpty();
    }

    @Test
    @DisplayName("Buscar por producto: lo encuentra o devuelve vacío")
    void buscarPorProducto() {
        repository.save(stock(10L, 5, 5));

        assertThat(repository.findByProductoId(10L)).isPresent();
        assertThat(repository.findByProductoId(99L)).isEmpty();
    }

    @Test
    @DisplayName("Producto duplicado: solo puede haber un registro de stock por producto")
    void productoDuplicado() {
        repository.saveAndFlush(stock(10L, 5, 5));

        assertThatThrownBy(() -> repository.saveAndFlush(stock(10L, 1, 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Alertas: devuelve los productos con stock igual o menor al mínimo")
    void alertas() {
        repository.save(stock(1L, 3, 5));
        repository.save(stock(2L, 5, 5));
        repository.save(stock(3L, 6, 5));

        assertThat(repository.findEnAlerta())
                .extracting(Stock::getProductoId)
                .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Buscar para actualizar: devuelve la fila con bloqueo, o vacío si no existe")
    void buscarParaActualizar() {
        repository.save(stock(10L, 5, 5));

        assertThat(repository.buscarParaActualizar(10L)).isPresent();
        assertThat(repository.buscarParaActualizar(99L)).isEmpty();
    }
}
