package com.mstock.inventario.service;

import com.mstock.inventario.client.CatalogoGateway;
import com.mstock.inventario.client.ProductoCatalogo;
import com.mstock.inventario.dto.ItemLote;
import com.mstock.inventario.dto.LoteRequest;
import com.mstock.inventario.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioFacadeLoteTest {

    private static final String TOKEN = "token";

    @Mock
    private CatalogoGateway catalogo;
    @Mock
    private InventarioService inventario;
    @InjectMocks
    private InventarioFacade facade;

    @Test
    @DisplayName("Salida en lote: valida cada producto distinto una sola vez y luego descuenta")
    void validaCadaProductoUnaVez() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(new ProductoCatalogo(1L, "A", "A", true));
        when(catalogo.consultarProducto(2L, TOKEN)).thenReturn(new ProductoCatalogo(2L, "B", "B", true));
        LoteRequest lote = new LoteRequest(
                List.of(new ItemLote(1L, 1), new ItemLote(2L, 1), new ItemLote(1L, 2)), "Venta");

        facade.registrarSalidaLote(lote, "cajero", TOKEN);

        verify(catalogo, times(1)).consultarProducto(1L, TOKEN);
        verify(catalogo, times(1)).consultarProducto(2L, TOKEN);
        verify(inventario).registrarSalidaLote(lote, "cajero");
    }

    @Test
    @DisplayName("Salida en lote: un producto inactivo rechaza todo el lote")
    void productoInactivoRechazaElLote() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(new ProductoCatalogo(1L, "A", "A", false));

        assertThatThrownBy(() -> facade.registrarSalidaLote(
                new LoteRequest(List.of(new ItemLote(1L, 1)), "Venta"), "cajero", TOKEN))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(inventario);
    }

    @Test
    @DisplayName("Reversión: no consulta el catálogo")
    void reversionNoConsultaElCatalogo() {
        LoteRequest lote = new LoteRequest(List.of(new ItemLote(1L, 1)), "Reversión");

        facade.registrarReversion(lote, "cajero");

        verifyNoInteractions(catalogo);
        verify(inventario).registrarEntradaLote(lote, "cajero");
    }
}
