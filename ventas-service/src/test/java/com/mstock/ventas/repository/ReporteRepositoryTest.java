package com.mstock.ventas.repository;

import com.mstock.ventas.entity.DetalleVenta;
import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.entity.Venta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:reportetest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ReporteRepositoryTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 17);
    private static final LocalDateTime DESDE = DIA.atStartOfDay();
    private static final LocalDateTime HASTA = DIA.plusDays(1).atStartOfDay();

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private DetalleVentaRepository detalleRepository;
    @Autowired
    private EntityManager em;

    /** Una línea: producto, cantidad, precio y costo (null = venta antigua sin costo). */
    private Venta venta(String usuario, LocalDateTime fecha, EstadoVenta estado,
                        long productoId, int cantidad, String precio, String costo) {
        BigDecimal p = new BigDecimal(precio);
        Venta v = Venta.builder().usuario(usuario).fecha(fecha).estado(estado)
                .total(p.multiply(BigDecimal.valueOf(cantidad))).build();
        v.agregarDetalle(DetalleVenta.builder().productoId(productoId).codigo("P-" + productoId)
                .nombre("Producto " + productoId).cantidad(cantidad).precioUnitario(p)
                .subtotal(p.multiply(BigDecimal.valueOf(cantidad)))
                .costoUnitario(costo == null ? null : new BigDecimal(costo)).build());
        return ventaRepository.saveAndFlush(v);
    }

    @Test
    @DisplayName("Resumir: cuenta y suma solo el estado y el rango pedidos")
    void resumir() {
        venta("ana", DESDE.plusHours(9), EstadoVenta.COMPLETADA, 1, 2, "5.00", "3.00");
        venta("ana", DESDE.plusHours(10), EstadoVenta.COMPLETADA, 1, 1, "5.00", "3.00");
        venta("ana", DESDE.plusHours(11), EstadoVenta.ANULADA, 1, 4, "5.00", "3.00");
        venta("ana", DESDE.minusDays(1), EstadoVenta.COMPLETADA, 1, 9, "5.00", "3.00");
        venta("ana", HASTA, EstadoVenta.COMPLETADA, 1, 9, "5.00", "3.00");
        em.clear();

        VentaRepository.ResumenVentas completadas = ventaRepository.resumir(EstadoVenta.COMPLETADA, DESDE, HASTA);
        VentaRepository.ResumenVentas anuladas = ventaRepository.resumir(EstadoVenta.ANULADA, DESDE, HASTA);

        assertThat(completadas.getCantidad()).isEqualTo(2);
        assertThat(completadas.getTotal()).isEqualByComparingTo("15.00");
        assertThat(anuladas.getCantidad()).isEqualTo(1);
        assertThat(anuladas.getTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("Resumir sin ventas: cantidad 0 y total null")
    void resumirVacio() {
        VentaRepository.ResumenVentas r = ventaRepository.resumir(EstadoVenta.COMPLETADA, DESDE, HASTA);

        assertThat(r.getCantidad()).isZero();
        assertThat(r.getTotal()).isNull();
    }

    @Test
    @DisplayName("Totales por cajero: agrupa por cajero y estado")
    void totalesPorCajero() {
        venta("luis", DESDE.plusHours(9), EstadoVenta.COMPLETADA, 1, 1, "10.00", "6.00");
        venta("ana", DESDE.plusHours(9), EstadoVenta.COMPLETADA, 1, 2, "10.00", "6.00");
        venta("ana", DESDE.plusHours(10), EstadoVenta.COMPLETADA, 1, 1, "10.00", "6.00");
        venta("ana", DESDE.plusHours(11), EstadoVenta.ANULADA, 1, 1, "10.00", "6.00");
        em.clear();

        List<VentaRepository.TotalCajero> filas = ventaRepository.totalesPorCajero(DESDE, HASTA);

        assertThat(filas).hasSize(3);
        assertThat(filas.get(0).getUsuario()).isEqualTo("ana");
        assertThat(filas).filteredOn(f -> f.getUsuario().equals("ana") && f.getEstado() == EstadoVenta.COMPLETADA)
                .singleElement().satisfies(f -> {
                    assertThat(f.getCantidad()).isEqualTo(2);
                    assertThat(f.getTotal()).isEqualByComparingTo("30.00");
                });
        assertThat(filas).filteredOn(f -> f.getEstado() == EstadoVenta.ANULADA).hasSize(1);
    }

    @Test
    @DisplayName("Más vendidos: por unidades, solo ventas completadas, respeta el límite")
    void masVendidos() {
        venta("ana", DESDE.plusHours(9), EstadoVenta.COMPLETADA, 1, 2, "5.00", "3.00");
        venta("ana", DESDE.plusHours(10), EstadoVenta.COMPLETADA, 2, 7, "2.00", "1.00");
        venta("ana", DESDE.plusHours(11), EstadoVenta.COMPLETADA, 1, 3, "5.00", "3.00");
        venta("ana", DESDE.plusHours(12), EstadoVenta.ANULADA, 3, 50, "1.00", "0.50");
        em.clear();

        List<DetalleVentaRepository.ProductoVendido> top =
                detalleRepository.masVendidos(DESDE, HASTA, PageRequest.of(0, 5));

        assertThat(top).extracting(DetalleVentaRepository.ProductoVendido::getProductoId).containsExactly(2L, 1L);
        assertThat(top.get(0).getCantidad()).isEqualTo(7);
        assertThat(top.get(1).getCantidad()).isEqualTo(5);
        assertThat(top.get(1).getIngresos()).isEqualByComparingTo("25.00");
        assertThat(detalleRepository.masVendidos(DESDE, HASTA, PageRequest.of(0, 1))).hasSize(1);
    }

    @Test
    @DisplayName("Rentabilidad: ganancia con costo; las líneas sin costo se cuentan aparte y las anuladas no entran")
    void rentabilidad() {
        venta("ana", DESDE.plusHours(9), EstadoVenta.COMPLETADA, 1, 2, "5.00", "3.00");   // 2 x 2.00 = 4.00
        venta("ana", DESDE.plusHours(10), EstadoVenta.COMPLETADA, 2, 3, "2.00", "1.50");  // 3 x 0.50 = 1.50
        venta("ana", DESDE.plusHours(11), EstadoVenta.COMPLETADA, 3, 5, "9.00", null);    // sin costo
        venta("ana", DESDE.plusHours(12), EstadoVenta.ANULADA, 1, 10, "5.00", "3.00");    // no cuenta
        em.clear();

        DetalleVentaRepository.Rentabilidad r = detalleRepository.rentabilidad(DESDE, HASTA);

        assertThat(r.getGanancia()).isEqualByComparingTo("5.50");
        assertThat(r.getLineasSinCosto()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rentabilidad sin ventas: ganancia null (el servicio la convierte en 0)")
    void rentabilidadVacia() {
        DetalleVentaRepository.Rentabilidad r = detalleRepository.rentabilidad(DESDE, HASTA);

        assertThat(r.getGanancia()).isNull();
    }
}
