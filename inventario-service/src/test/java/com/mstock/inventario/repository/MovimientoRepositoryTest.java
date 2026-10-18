package com.mstock.inventario.repository;

import com.mstock.inventario.entity.Movimiento;
import com.mstock.inventario.entity.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:movimientotest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class MovimientoRepositoryTest {

    @Autowired
    private MovimientoRepository repository;

    private Movimiento movimiento(long productoId, TipoMovimiento tipo, int cantidad,
                                  int anterior, int resultante, LocalDateTime fecha) {
        return Movimiento.builder()
                .productoId(productoId)
                .tipo(tipo)
                .cantidad(cantidad)
                .stockAnterior(anterior)
                .stockResultante(resultante)
                .motivo("Prueba")
                .usuario("admin")
                .fecha(fecha)
                .build();
    }

    @Test
    @DisplayName("Insertar: guarda el movimiento y asigna id y fecha")
    void insertar() {
        Movimiento guardado = repository.save(
                movimiento(10L, TipoMovimiento.ENTRADA, 20, 0, 20, null));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFecha()).isNotNull();
        assertThat(guardado.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
    }

    @Test
    @DisplayName("Kardex: devuelve los movimientos del producto del más reciente al más antiguo")
    void kardexOrdenado() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 8, 0);
        repository.save(movimiento(10L, TipoMovimiento.ENTRADA, 20, 0, 20, base));
        repository.save(movimiento(10L, TipoMovimiento.SALIDA, 5, 20, 15, base.plusMinutes(10)));
        repository.save(movimiento(10L, TipoMovimiento.AJUSTE, 1, 15, 14, base.plusMinutes(20)));
        repository.save(movimiento(99L, TipoMovimiento.ENTRADA, 7, 0, 7, base.plusMinutes(30)));

        List<Movimiento> kardex = repository.findByProductoIdOrderByFechaDescIdDesc(10L);

        assertThat(kardex).extracting(Movimiento::getTipo)
                .containsExactly(TipoMovimiento.AJUSTE, TipoMovimiento.SALIDA, TipoMovimiento.ENTRADA);
    }

    @Test
    @DisplayName("Contar: cuenta solo los movimientos del producto")
    void contar() {
        repository.save(movimiento(10L, TipoMovimiento.ENTRADA, 20, 0, 20, null));
        repository.save(movimiento(10L, TipoMovimiento.SALIDA, 5, 20, 15, null));
        repository.save(movimiento(99L, TipoMovimiento.ENTRADA, 7, 0, 7, null));

        assertThat(repository.countByProductoId(10L)).isEqualTo(2);
        assertThat(repository.countByProductoId(99L)).isEqualTo(1);
        assertThat(repository.countByProductoId(1L)).isZero();
    }

    @Test
    @DisplayName("Actualizar: se puede corregir el motivo de un movimiento")
    void actualizar() {
        Movimiento guardado = repository.save(
                movimiento(10L, TipoMovimiento.ENTRADA, 20, 0, 20, null));

        guardado.setMotivo("Compra a proveedor");
        repository.saveAndFlush(guardado);

        assertThat(repository.findById(guardado.getId()).orElseThrow().getMotivo())
                .isEqualTo("Compra a proveedor");
    }

    @Test
    @DisplayName("Eliminar: borra el movimiento")
    void eliminar() {
        Movimiento guardado = repository.save(
                movimiento(10L, TipoMovimiento.ENTRADA, 20, 0, 20, null));

        repository.deleteById(guardado.getId());
        repository.flush();

        assertThat(repository.findById(guardado.getId())).isEmpty();
    }

    @Test
    @DisplayName("Buscar por operación: devuelve solo los movimientos de esa operación, en orden")
    void buscarPorOperacion() {
        Movimiento a = movimiento(1L, TipoMovimiento.SALIDA, 3, 10, 7, LocalDateTime.now());
        a.setOperacionId("op-0001-test");
        Movimiento b = movimiento(2L, TipoMovimiento.SALIDA, 2, 5, 3, LocalDateTime.now());
        b.setOperacionId("op-0001-test");
        Movimiento otra = movimiento(1L, TipoMovimiento.SALIDA, 1, 7, 6, LocalDateTime.now());
        otra.setOperacionId("op-0002-test");
        repository.saveAll(List.of(a, b, otra));

        List<Movimiento> encontrados = repository.findByOperacionIdOrderByIdAsc("op-0001-test");

        assertThat(encontrados).extracting(Movimiento::getProductoId).containsExactly(1L, 2L);
        assertThat(repository.findByOperacionIdOrderByIdAsc("no-existe-xx")).isEmpty();
    }
}
