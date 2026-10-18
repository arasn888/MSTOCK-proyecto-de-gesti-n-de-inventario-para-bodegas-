package com.mstock.inventario.service;

import com.mstock.inventario.client.CatalogoGateway;
import com.mstock.inventario.client.ProductoCatalogo;
import com.mstock.inventario.dto.AjusteRequest;
import com.mstock.inventario.dto.MovimientoRequest;
import com.mstock.inventario.exception.RecursoNoEncontradoException;
import com.mstock.inventario.exception.ReglaNegocioException;
import com.mstock.inventario.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioFacadeTest {

    private static final String TOKEN = "token-del-usuario";

    @Mock
    private CatalogoGateway catalogo;
    @Mock
    private InventarioService inventario;
    @InjectMocks
    private InventarioFacade facade;

    private final MovimientoRequest movimiento = new MovimientoRequest(7L, 10, "Prueba");

    @Test
    @DisplayName("Producto activo: valida en el catálogo y registra la entrada")
    void entradaConProductoActivo() {
        when(catalogo.consultarProducto(7L, TOKEN))
                .thenReturn(new ProductoCatalogo(7L, "ARR-001", "Arroz", true));

        facade.registrarEntrada(movimiento, "almacenero", TOKEN);

        verify(inventario).registrarEntrada(movimiento, "almacenero");
    }

    @Test
    @DisplayName("Producto activo: la salida también se valida antes de descontar")
    void salidaConProductoActivo() {
        when(catalogo.consultarProducto(7L, TOKEN))
                .thenReturn(new ProductoCatalogo(7L, "ARR-001", "Arroz", true));

        facade.registrarSalida(movimiento, "cajero", TOKEN);

        verify(inventario).registrarSalida(movimiento, "cajero");
    }

    @Test
    @DisplayName("Producto activo: el ajuste se valida antes de corregir el stock")
    void ajusteConProductoActivo() {
        AjusteRequest ajuste = new AjusteRequest(7L, 12, "Conteo físico");
        when(catalogo.consultarProducto(7L, TOKEN))
                .thenReturn(new ProductoCatalogo(7L, "ARR-001", "Arroz", true));

        facade.registrarAjuste(ajuste, "almacenero", TOKEN);

        verify(inventario).registrarAjuste(ajuste, "almacenero");
    }

    @Test
    @DisplayName("Producto inactivo: se rechaza y no se toca el inventario")
    void productoInactivoSeRechaza() {
        when(catalogo.consultarProducto(7L, TOKEN))
                .thenReturn(new ProductoCatalogo(7L, "ARR-001", "Arroz", false));

        assertThatThrownBy(() -> facade.registrarSalida(movimiento, "cajero", TOKEN))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactivo");
        verifyNoInteractions(inventario);
    }

    @Test
    @DisplayName("Producto inexistente en el catálogo: 404 y no se toca el inventario")
    void productoInexistenteSeRechaza() {
        when(catalogo.consultarProducto(7L, TOKEN))
                .thenThrow(new RecursoNoEncontradoException("El producto 7 no existe en el catálogo"));

        assertThatThrownBy(() -> facade.registrarEntrada(movimiento, "almacenero", TOKEN))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(inventario);
    }

    @Test
    @DisplayName("Catálogo caído: la operación falla y no se mueve stock")
    void catalogoCaidoNoMueveStock() {
        when(catalogo.consultarProducto(7L, TOKEN))
                .thenThrow(new ServicioNoDisponibleException("catálogo caído"));

        assertThatThrownBy(() -> facade.registrarSalida(movimiento, "cajero", TOKEN))
                .isInstanceOf(ServicioNoDisponibleException.class);
        verify(inventario, never()).registrarSalida(any(), any());
    }
}
