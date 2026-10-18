package com.mstock.ventas.repository;

import com.mstock.ventas.entity.Cliente;
import com.mstock.ventas.entity.DetalleVenta;
import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.entity.Venta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:ventatest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class VentaRepositoryTest {

    @Autowired
    private VentaRepository repository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EntityManager em;

    private Venta venta(Cliente cliente, LocalDateTime fecha, String... codigos) {
        Venta v = Venta.builder().usuario("cajero").cliente(cliente).fecha(fecha).total(BigDecimal.ZERO).build();
        BigDecimal total = BigDecimal.ZERO;
        long productoId = 1;
        for (String codigo : codigos) {
            BigDecimal subtotal = new BigDecimal("3.50").multiply(BigDecimal.valueOf(2));
            v.agregarDetalle(DetalleVenta.builder().productoId(productoId++).codigo(codigo).nombre("Producto " + codigo)
                    .cantidad(2).precioUnitario(new BigDecimal("3.50")).subtotal(subtotal).build());
            total = total.add(subtotal);
        }
        v.setTotal(total);
        return v;
    }

    @Test
    @DisplayName("Insertar: guarda la venta con su detalle y estado COMPLETADA")
    void insertarConDetalle() {
        Venta guardada = repository.saveAndFlush(venta(null, LocalDateTime.now(), "A-1", "B-2"));
        em.clear();

        Venta leida = repository.buscarConDetalle(guardada.getId()).orElseThrow();

        assertThat(leida.getEstado()).isEqualTo(EstadoVenta.COMPLETADA);
        assertThat(leida.getDetalles()).hasSize(2);
        assertThat(leida.getTotal()).isEqualByComparingTo("14.00");
    }

    @Test
    @DisplayName("Actualizar: se puede asignar el número de comprobante")
    void actualizarNumero() {
        Venta guardada = repository.saveAndFlush(venta(null, LocalDateTime.now(), "A-1"));
        guardada.setNumero("B001-00000001");
        repository.saveAndFlush(guardada);
        em.clear();

        assertThat(repository.findById(guardada.getId()).orElseThrow().getNumero()).isEqualTo("B001-00000001");
    }

    @Test
    @DisplayName("Eliminar: al borrar la venta se borra también su detalle")
    void eliminarEnCascada() {
        Venta guardada = repository.saveAndFlush(venta(null, LocalDateTime.now(), "A-1", "B-2"));
        em.clear();

        repository.deleteById(guardada.getId());
        repository.flush();

        assertThat(repository.findById(guardada.getId())).isEmpty();
        Long detalles = em.createQuery("select count(d) from DetalleVenta d", Long.class).getSingleResult();
        assertThat(detalles).isZero();
    }

    @Test
    @DisplayName("Listar por rango: incluye el inicio, excluye el fin y ordena de la más reciente a la más antigua")
    void listarPorRango() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 10, 0, 0);
        repository.save(venta(null, base.minusSeconds(1), "FUERA-ANTES"));
        Venta primera = repository.save(venta(null, base, "A-1"));
        Venta segunda = repository.save(venta(null, base.plusHours(5), "A-2"));
        repository.save(venta(null, base.plusDays(1), "FUERA-DESPUES"));
        repository.flush();

        List<Venta> ventas = repository.buscarPorRango(base, base.plusDays(1));

        assertThat(ventas).extracting(Venta::getId).containsExactly(segunda.getId(), primera.getId());
    }

    @Test
    @DisplayName("La venta guarda la referencia al cliente")
    void conCliente() {
        Cliente cliente = clienteRepository.saveAndFlush(
                Cliente.builder().documento("12345678").nombres("Ana Torres").build());
        Venta guardada = repository.saveAndFlush(venta(cliente, LocalDateTime.now(), "A-1"));
        em.clear();

        Venta leida = repository.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getCliente().getId()).isEqualTo(cliente.getId());
    }

    @Test
    @DisplayName("Anular: pasa a ANULADA con motivo y usuario solo si estaba COMPLETADA")
    void anularSiCompletada() {
        Venta guardada = repository.saveAndFlush(venta(null, LocalDateTime.now(), "A-1"));
        LocalDateTime ahora = LocalDateTime.now();

        int primera = repository.anularSiCompletada(guardada.getId(), "Error de digitación", "admin", ahora);
        int segunda = repository.anularSiCompletada(guardada.getId(), "Otro motivo", "otro", ahora);
        em.clear();

        Venta leida = repository.buscarConDetalle(guardada.getId()).orElseThrow();
        assertThat(primera).isEqualTo(1);
        assertThat(segunda).isZero();
        assertThat(leida.getEstado()).isEqualTo(EstadoVenta.ANULADA);
        // La segunda anulación no pisó los datos de la primera.
        assertThat(leida.getMotivoAnulacion()).isEqualTo("Error de digitación");
        assertThat(leida.getUsuarioAnulacion()).isEqualTo("admin");
        assertThat(leida.getFechaAnulacion()).isNotNull();
    }

    @Test
    @DisplayName("El id de operación guarda y no se repite entre ventas")
    void operacionIdUnico() {
        Venta a = venta(null, LocalDateTime.now(), "A-1");
        a.setOperacionId("op-unica-0001");
        repository.saveAndFlush(a);
        Venta b = venta(null, LocalDateTime.now(), "B-2");
        b.setOperacionId("op-unica-0001");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.saveAndFlush(b))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
