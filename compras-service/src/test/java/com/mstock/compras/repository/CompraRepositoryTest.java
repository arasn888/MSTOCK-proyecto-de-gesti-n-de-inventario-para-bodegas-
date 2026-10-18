package com.mstock.compras.repository;

import com.mstock.compras.entity.Compra;
import com.mstock.compras.entity.DetalleCompra;
import com.mstock.compras.entity.EstadoCompra;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:comprastest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class CompraRepositoryTest {

    @Autowired
    private CompraRepository repository;
    @Autowired
    private EntityManager em;

    private Compra compra(long proveedorId, String... productos) {
        Compra compra = new Compra();
        compra.setProveedorId(proveedorId);
        compra.setUsuario("almacenero");
        BigDecimal total = BigDecimal.ZERO;
        long productoId = 1;
        for (String nombre : productos) {
            DetalleCompra d = new DetalleCompra();
            d.setProductoId(productoId++);
            d.setProductoNombre(nombre);
            d.setCantidad(10);
            d.setCostoUnitario(new BigDecimal("2.50"));
            d.setSubtotal(new BigDecimal("25.00"));
            compra.agregarDetalle(d);
            total = total.add(d.getSubtotal());
        }
        compra.setTotal(total);
        return compra;
    }

    @Test
    @DisplayName("Insertar: guarda la compra con su detalle, estado PENDIENTE y fecha")
    void insertar() {
        Compra guardada = repository.saveAndFlush(compra(1L, "Arroz", "Azúcar"));
        em.clear();

        Compra leida = repository.findById(guardada.getId()).orElseThrow();

        assertThat(leida.getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(leida.getFecha()).isNotNull();
        assertThat(leida.getDetalles()).hasSize(2);
        assertThat(leida.getTotal()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Listar: devuelve todas las compras")
    void listar() {
        repository.save(compra(1L, "Arroz"));
        repository.save(compra(2L, "Azúcar"));
        repository.flush();

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Actualizar: se puede corregir el usuario y el total de la compra")
    void actualizar() {
        Compra guardada = repository.saveAndFlush(compra(1L, "Arroz"));
        guardada.setUsuario("admin");
        guardada.setTotal(new BigDecimal("99.00"));
        repository.saveAndFlush(guardada);
        em.clear();

        Compra leida = repository.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getUsuario()).isEqualTo("admin");
        assertThat(leida.getTotal()).isEqualByComparingTo("99.00");
    }

    @Test
    @DisplayName("Eliminar: al borrar la compra se borra su detalle")
    void eliminarEnCascada() {
        Compra guardada = repository.saveAndFlush(compra(1L, "Arroz", "Azúcar"));
        em.clear();

        repository.deleteById(guardada.getId());
        repository.flush();

        assertThat(repository.findById(guardada.getId())).isEmpty();
        Long detalles = em.createQuery("select count(d) from DetalleCompra d", Long.class).getSingleResult();
        assertThat(detalles).isZero();
    }
}
