package com.mstock.inventario.service;

import com.mstock.inventario.client.CatalogoGateway;
import com.mstock.inventario.client.ProductoCatalogo;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.entity.TipoMovimiento;
import com.mstock.inventario.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioFacadeKardexTest {

    @Mock
    private CatalogoGateway catalogo;
    @Mock
    private InventarioService inventario;
    @InjectMocks
    private InventarioFacade facade;

    @Test
    @DisplayName("Exportar: toma código y nombre del catálogo y escribe los movimientos del más antiguo al más reciente")
    void exportaEnOrdenCronologico() {
        when(catalogo.consultarProducto(1L, "tok")).thenReturn(new ProductoCatalogo(1L, "ARR-1", "Arroz", true));
        // El servicio entrega el kardex del más reciente al más antiguo.
        when(inventario.kardex(1L)).thenReturn(List.of(
                new MovimientoResponse(2L, 1L, TipoMovimiento.SALIDA, 3, 10, 7, "Venta reciente", "cajero",
                        LocalDateTime.of(2026, 10, 2, 10, 0)),
                new MovimientoResponse(1L, 1L, TipoMovimiento.ENTRADA, 10, 0, 10, "Stock antiguo", "admin",
                        LocalDateTime.of(2026, 10, 1, 10, 0))));

        KardexCsv.Archivo archivo = facade.exportarKardex(1L, "tok");

        String csv = new String(archivo.contenido(), StandardCharsets.UTF_8);
        assertThat(archivo.nombre()).isEqualTo("kardex-ARR-1.csv");
        assertThat(csv).contains("ARR-1 - Arroz");
        assertThat(csv.indexOf("Stock antiguo")).isLessThan(csv.indexOf("Venta reciente"));
        verify(catalogo).consultarProducto(1L, "tok");
    }

    @Test
    @DisplayName("Exportar un producto que no existe en el catálogo: 404 y no se consulta el inventario")
    void productoInexistente() {
        when(catalogo.consultarProducto(99L, "tok"))
                .thenThrow(new RecursoNoEncontradoException("El producto 99 no existe en el catálogo"));

        assertThatThrownBy(() -> facade.exportarKardex(99L, "tok")).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(inventario);
    }
}
