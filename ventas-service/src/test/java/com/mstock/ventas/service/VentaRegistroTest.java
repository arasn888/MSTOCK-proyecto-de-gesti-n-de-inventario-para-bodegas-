package com.mstock.ventas.service;

import com.mstock.ventas.client.ProductoCatalogo;
import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.entity.Venta;
import com.mstock.ventas.repository.ClienteRepository;
import com.mstock.ventas.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaRegistroTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @InjectMocks
    private VentaRegistro registro;

    @Test
    @DisplayName("Calcula subtotales y total con el precio del catálogo y asigna el número de comprobante")
    void calculaTotalYNumero() {
        when(ventaRepository.saveAndFlush(any(Venta.class))).thenAnswer(invocacion -> {
            Venta v = invocacion.getArgument(0);
            if (v.getId() == null) {
                v.setId(42L);
            }
            return v;
        });
        List<LineaVenta> lineas = List.of(
                new LineaVenta(new ProductoCatalogo(1L, "ARR-1", "Arroz", new BigDecimal("3.50"), true), 2),
                new LineaVenta(new ProductoCatalogo(2L, "AZU-1", "Azúcar", new BigDecimal("4.20"), true), 3));

        VentaResponse respuesta = registro.guardar(null, "cajero", lineas);

        assertThat(respuesta.numero()).isEqualTo("B001-00000042");
        assertThat(respuesta.total()).isEqualByComparingTo("19.60");
        assertThat(respuesta.detalle()).hasSize(2);
        assertThat(respuesta.detalle().get(0).subtotal()).isEqualByComparingTo("7.00");
        assertThat(respuesta.detalle().get(1).subtotal()).isEqualByComparingTo("12.60");
        assertThat(respuesta.usuario()).isEqualTo("cajero");
    }
}
