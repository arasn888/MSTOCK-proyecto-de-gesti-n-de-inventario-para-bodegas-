package com.mstock.ventas.service;

import com.mstock.ventas.dto.CierreCajaResponse;
import com.mstock.ventas.dto.DashboardResponse;
import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.repository.DetalleVentaRepository;
import com.mstock.ventas.repository.DetalleVentaRepository.ProductoVendido;
import com.mstock.ventas.repository.DetalleVentaRepository.Rentabilidad;
import com.mstock.ventas.repository.VentaRepository;
import com.mstock.ventas.repository.VentaRepository.ResumenVentas;
import com.mstock.ventas.repository.VentaRepository.TotalCajero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 17);

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private DetalleVentaRepository detalleRepository;
    @InjectMocks
    private ReporteService service;

    // Implementaciones simples de las proyecciones (no mocks: Mockito no permite crear un mock
    // con when(...) dentro de otro when(...).thenReturn(...)).
    private ResumenVentas resumen(Long cantidad, String total) {
        BigDecimal monto = total == null ? null : new BigDecimal(total);
        return new ResumenVentas() {
            public Long getCantidad() { return cantidad; }
            public BigDecimal getTotal() { return monto; }
        };
    }

    private Rentabilidad renta(String ganancia, Long sinCosto) {
        BigDecimal monto = ganancia == null ? null : new BigDecimal(ganancia);
        return new Rentabilidad() {
            public BigDecimal getGanancia() { return monto; }
            public Long getLineasSinCosto() { return sinCosto; }
        };
    }

    private TotalCajero fila(String usuario, EstadoVenta estado, long cantidad, String total) {
        BigDecimal monto = new BigDecimal(total);
        return new TotalCajero() {
            public String getUsuario() { return usuario; }
            public EstadoVenta getEstado() { return estado; }
            public Long getCantidad() { return cantidad; }
            public BigDecimal getTotal() { return monto; }
        };
    }

    private ProductoVendido producto(long id, String codigo, String nombre, long cantidad, String ingresos) {
        BigDecimal monto = new BigDecimal(ingresos);
        return new ProductoVendido() {
            public Long getProductoId() { return id; }
            public String getCodigo() { return codigo; }
            public String getNombre() { return nombre; }
            public Long getCantidad() { return cantidad; }
            public BigDecimal getIngresos() { return monto; }
        };
    }

    @Test
    @DisplayName("Dashboard: total, ticket promedio, ganancia, anuladas aparte y más vendidos")
    void dashboard() {
        LocalDateTime desde = DIA.atStartOfDay();
        LocalDateTime hasta = DIA.plusDays(1).atStartOfDay();
        when(ventaRepository.resumir(EstadoVenta.COMPLETADA, desde, hasta)).thenReturn(resumen(3L, "100.00"));
        when(ventaRepository.resumir(EstadoVenta.ANULADA, desde, hasta)).thenReturn(resumen(1L, "20.50"));
        when(detalleRepository.rentabilidad(desde, hasta)).thenReturn(renta("35.40", 2L));
        ProductoVendido arroz = producto(1L, "ARR-1", "Arroz", 12L, "42");
        when(detalleRepository.masVendidos(eq(desde), eq(hasta), any(Pageable.class))).thenReturn(List.of(arroz));

        DashboardResponse r = service.dashboard(DIA, DIA, null);

        assertThat(r.ventas().cantidad()).isEqualTo(3);
        assertThat(r.ventas().total()).isEqualByComparingTo("100.00");
        assertThat(r.ventas().ticketPromedio()).isEqualByComparingTo("33.33");
        assertThat(r.ventas().ganancia()).isEqualByComparingTo("35.40");
        assertThat(r.ventas().lineasSinCosto()).isEqualTo(2);
        assertThat(r.anuladas().cantidad()).isEqualTo(1);
        assertThat(r.anuladas().total()).isEqualByComparingTo("20.50");
        assertThat(r.masVendidos()).hasSize(1);
        assertThat(r.masVendidos().get(0).unidadesVendidas()).isEqualTo(12);
        assertThat(r.masVendidos().get(0).ingresos()).isEqualByComparingTo("42.00");
    }

    @Test
    @DisplayName("Dashboard sin ventas: ceros (no nulos ni división por cero)")
    void dashboardVacio() {
        when(ventaRepository.resumir(eq(EstadoVenta.COMPLETADA), any(), any())).thenReturn(resumen(0L, null));
        when(ventaRepository.resumir(eq(EstadoVenta.ANULADA), any(), any())).thenReturn(resumen(0L, null));
        when(detalleRepository.rentabilidad(any(), any())).thenReturn(renta(null, null));
        when(detalleRepository.masVendidos(any(), any(), any(Pageable.class))).thenReturn(List.of());

        DashboardResponse r = service.dashboard(DIA, DIA, null);

        assertThat(r.ventas().cantidad()).isZero();
        assertThat(r.ventas().total()).isEqualByComparingTo("0.00");
        assertThat(r.ventas().ticketPromedio()).isEqualByComparingTo("0.00");
        assertThat(r.ventas().ganancia()).isEqualByComparingTo("0.00");
        assertThat(r.ventas().lineasSinCosto()).isZero();
        assertThat(r.masVendidos()).isEmpty();
    }

    @Test
    @DisplayName("Dashboard: el rango incluye el día final (hasta es exclusivo al día siguiente)")
    void rangoIncluyeElUltimoDia() {
        when(ventaRepository.resumir(eq(EstadoVenta.COMPLETADA), any(), any())).thenReturn(resumen(0L, null));
        when(ventaRepository.resumir(eq(EstadoVenta.ANULADA), any(), any())).thenReturn(resumen(0L, null));
        when(detalleRepository.rentabilidad(any(), any())).thenReturn(renta(null, null));
        when(detalleRepository.masVendidos(any(), any(), any(Pageable.class))).thenReturn(List.of());

        service.dashboard(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 17), 3);

        verify(ventaRepository).resumir(EstadoVenta.COMPLETADA,
                LocalDate.of(2026, 10, 1).atStartOfDay(), LocalDate.of(2026, 10, 18).atStartOfDay());
    }

    @Test
    @DisplayName("Dashboard: valida rango invertido, rango mayor a un año y top fuera de 1-50")
    void validaciones() {
        assertThatThrownBy(() -> service.dashboard(DIA, DIA.minusDays(1), null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("posterior");
        assertThatThrownBy(() -> service.dashboard(DIA.minusDays(400), DIA, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("366");
        assertThatThrownBy(() -> service.dashboard(DIA, DIA, 0))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("top");
        assertThatThrownBy(() -> service.dashboard(DIA, DIA, 51))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("top");
    }

    @Test
    @DisplayName("Cierre de caja: junta vendidas y anuladas por cajero y calcula los totales")
    void cierreCaja() {
        when(ventaRepository.totalesPorCajero(DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay())).thenReturn(List.of(
                fila("ana", EstadoVenta.COMPLETADA, 4, "80.00"),
                fila("ana", EstadoVenta.ANULADA, 1, "10.00"),
                fila("luis", EstadoVenta.COMPLETADA, 2, "30.50")));

        CierreCajaResponse r = service.cierreCaja(DIA, null);

        assertThat(r.fecha()).isEqualTo(DIA);
        assertThat(r.cajeros()).hasSize(2);
        CierreCajaResponse.Cajero ana = r.cajeros().get(0);
        assertThat(ana.usuario()).isEqualTo("ana");
        assertThat(ana.cantidadVentas()).isEqualTo(4);
        assertThat(ana.totalVendido()).isEqualByComparingTo("80.00");
        assertThat(ana.cantidadAnuladas()).isEqualTo(1);
        assertThat(ana.totalAnulado()).isEqualByComparingTo("10.00");
        CierreCajaResponse.Cajero luis = r.cajeros().get(1);
        assertThat(luis.cantidadAnuladas()).isZero();
        assertThat(luis.totalAnulado()).isEqualByComparingTo("0.00");
        assertThat(r.totales().cantidadVentas()).isEqualTo(6);
        assertThat(r.totales().totalVendido()).isEqualByComparingTo("110.50");
        assertThat(r.totales().cantidadAnuladas()).isEqualTo(1);
        assertThat(r.totales().totalAnulado()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Cierre de caja de un cajero: solo su fila, sin los demás")
    void cierreSoloDeUnCajero() {
        when(ventaRepository.totalesPorCajero(any(), any())).thenReturn(List.of(
                fila("ana", EstadoVenta.COMPLETADA, 4, "80.00"),
                fila("luis", EstadoVenta.COMPLETADA, 2, "30.50")));

        CierreCajaResponse r = service.cierreCaja(DIA, "luis");

        assertThat(r.cajeros()).extracting(CierreCajaResponse.Cajero::usuario).containsExactly("luis");
        assertThat(r.totales().totalVendido()).isEqualByComparingTo("30.50");
    }

    @Test
    @DisplayName("Cierre de caja sin ventas: lista vacía y totales en cero")
    void cierreVacio() {
        when(ventaRepository.totalesPorCajero(any(), any())).thenReturn(List.of());

        CierreCajaResponse r = service.cierreCaja(DIA, null);

        assertThat(r.cajeros()).isEmpty();
        assertThat(r.totales().totalVendido()).isEqualByComparingTo("0.00");
    }
}
