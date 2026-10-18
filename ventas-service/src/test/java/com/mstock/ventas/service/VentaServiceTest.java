package com.mstock.ventas.service;

import com.mstock.ventas.client.CatalogoGateway;
import com.mstock.ventas.client.InventarioGateway;
import com.mstock.ventas.client.ItemLote;
import com.mstock.ventas.client.ProductoCatalogo;
import com.mstock.ventas.config.TokenServicio;
import com.mstock.ventas.dto.ItemVentaRequest;
import com.mstock.ventas.dto.VentaRequest;
import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ResultadoIncierto;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.exception.ServicioNoDisponibleException;
import com.mstock.ventas.exception.StockInsuficienteException;
import com.mstock.ventas.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final String TOKEN = "token";

    @Mock
    private ClienteService clienteService;
    @Mock
    private CatalogoGateway catalogo;
    @Mock
    private InventarioGateway inventario;
    @Mock
    private VentaRegistro registro;
    @Mock
    private VentaAnulacion anulacion;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private TokenServicio tokenServicio;
    @InjectMocks
    private VentaService service;

    private ProductoCatalogo producto(long id, String precio, boolean activo) {
        return new ProductoCatalogo(id, "COD-" + id, "Producto " + id, new BigDecimal(precio), activo);
    }

    private VentaRequest venta(Long clienteId, ItemVentaRequest... items) {
        return new VentaRequest(clienteId, List.of(items));
    }

    @Test
    @DisplayName("Venta correcta: toma el precio del catálogo, descuenta el stock y guarda")
    void ventaCorrecta() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));

        service.registrar(venta(null, new ItemVentaRequest(1L, 2)), "cajero", TOKEN);

        ArgumentCaptor<String> operacion = ArgumentCaptor.forClass(String.class);
        verify(inventario).descontar(operacion.capture(), eq(List.of(new ItemLote(1L, 2))), eq(TOKEN));
        assertThat(java.util.UUID.fromString(operacion.getValue())).isNotNull();
        ArgumentCaptor<List<LineaVenta>> lineas = ArgumentCaptor.forClass(List.class);
        // La misma operación que descontó el stock queda guardada en la venta (para poder anularla).
        verify(registro).guardar(eq(null), eq("cajero"), eq(operacion.getValue()), lineas.capture());
        assertThat(lineas.getValue()).hasSize(1);
        assertThat(lineas.getValue().get(0).producto().precioVenta()).isEqualByComparingTo("3.50");
        assertThat(lineas.getValue().get(0).cantidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("Productos repetidos: se unen y se consulta cada producto una sola vez")
    void unePorProducto() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));
        when(catalogo.consultarProducto(2L, TOKEN)).thenReturn(producto(2, "1.00", true));

        service.registrar(venta(null, new ItemVentaRequest(2L, 1), new ItemVentaRequest(1L, 2),
                new ItemVentaRequest(2L, 4)), "cajero", TOKEN);

        verify(inventario).descontar(anyString(), eq(List.of(new ItemLote(1L, 2), new ItemLote(2L, 5))), eq(TOKEN));
    }

    @Test
    @DisplayName("Producto inactivo: se rechaza antes de tocar el inventario")
    void productoInactivo() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", false));

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 1)), "cajero", TOKEN))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("inactivo");
        verifyNoInteractions(inventario, registro);
    }

    @Test
    @DisplayName("Producto inexistente en el catálogo: 404 y no se toca el inventario")
    void productoInexistente() {
        when(catalogo.consultarProducto(1L, TOKEN))
                .thenThrow(new RecursoNoEncontradoException("El producto 1 no existe en el catálogo"));

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 1)), "cajero", TOKEN))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(inventario, registro);
    }

    @Test
    @DisplayName("Stock insuficiente: no se guarda nada y no hay nada que revertir")
    void stockInsuficiente() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));
        doThrow(new StockInsuficienteException("Stock insuficiente: disponible 1, solicitado 5"))
                .when(inventario).descontar(anyString(), anyList(), eq(TOKEN));

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 5)), "cajero", TOKEN))
                .isInstanceOf(StockInsuficienteException.class);
        verify(registro, never()).guardar(any(), any(), any(), anyList());
        verify(inventario, never()).revertirOperacion(any(), any(), any());
    }

    @Test
    @DisplayName("Inventario caído: 503 y la venta no se guarda")
    void inventarioCaido() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));
        doThrow(new ServicioNoDisponibleException("inventario caído"))
                .when(inventario).descontar(anyString(), anyList(), eq(TOKEN));

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 1)), "cajero", TOKEN))
                .isInstanceOf(ServicioNoDisponibleException.class);
        verify(registro, never()).guardar(any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Compensación: si guardar falla tras descontar, se devuelve el stock")
    void falloAlGuardarRevierteElStock() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));
        when(registro.guardar(any(), any(), any(), anyList())).thenThrow(new IllegalStateException("BD caída"));
        when(tokenServicio.generar()).thenReturn("token-servicio");

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 2)), "cajero", TOKEN))
                .isInstanceOf(IllegalStateException.class).hasMessage("BD caída");
        // Se deshace por id de operación y con el token de la cuenta de servicio, no con el del cajero.
        ArgumentCaptor<String> operacion = ArgumentCaptor.forClass(String.class);
        verify(inventario).descontar(operacion.capture(), anyList(), eq(TOKEN));
        verify(inventario).revertirOperacion(eq(operacion.getValue()), anyString(), eq("token-servicio"));
    }

    @Test
    @DisplayName("Si además falla la reversión, se informa el error original de la venta")
    void falloLaReversion() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));
        when(registro.guardar(any(), any(), any(), anyList())).thenThrow(new IllegalStateException("BD caída"));
        doThrow(new ServicioNoDisponibleException("inventario caído"))
                .when(inventario).revertirOperacion(any(), any(), any());

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 2)), "cajero", TOKEN))
                .isInstanceOf(IllegalStateException.class).hasMessage("BD caída");
    }

    @Test
    @DisplayName("Cliente inactivo o inexistente: se rechaza antes de consultar nada")
    void clienteInvalido() {
        when(clienteService.buscarActivo(7L)).thenThrow(new ReglaNegocioException("El cliente indicado está inactivo"));

        assertThatThrownBy(() -> service.registrar(venta(7L, new ItemVentaRequest(1L, 1)), "cajero", TOKEN))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(catalogo, inventario, registro);
    }

    @Test
    @DisplayName("Listar: la fecha inicial no puede ser posterior a la final")
    void rangoInvalido() {
        assertThatThrownBy(() -> service.listar(java.time.LocalDate.of(2026, 10, 10), java.time.LocalDate.of(2026, 10, 1)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Timeout del inventario: no se sabe si descontó, se deshace por id de operación y se informa 503")
    void resultadoIncierto() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));
        doThrow(new ResultadoIncierto("el inventario no respondió"))
                .when(inventario).descontar(anyString(), anyList(), eq(TOKEN));
        when(tokenServicio.generar()).thenReturn("token-servicio");

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 2)), "cajero", TOKEN))
                .isInstanceOf(ResultadoIncierto.class);

        ArgumentCaptor<String> operacion = ArgumentCaptor.forClass(String.class);
        verify(inventario).descontar(operacion.capture(), anyList(), eq(TOKEN));
        verify(inventario).revertirOperacion(eq(operacion.getValue()), anyString(), eq("token-servicio"));
        verify(registro, never()).guardar(any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Inventario con el circuito abierto: la petición nunca salió, no hay nada que deshacer")
    void circuitoAbiertoNoCompensa() {
        when(catalogo.consultarProducto(1L, TOKEN)).thenReturn(producto(1, "3.50", true));
        doThrow(new ServicioNoDisponibleException("no está respondiendo"))
                .when(inventario).descontar(anyString(), anyList(), eq(TOKEN));

        assertThatThrownBy(() -> service.registrar(venta(null, new ItemVentaRequest(1L, 2)), "cajero", TOKEN))
                .isInstanceOf(ServicioNoDisponibleException.class);

        verify(inventario, never()).revertirOperacion(any(), any(), any());
    }

    // ---------------------------------------------------------------- anulación

    private VentaResponse respuesta() {
        return new VentaResponse(5L, "B001-00000005", null, null, "cajero", java.time.LocalDateTime.now(),
                new BigDecimal("13.00"), EstadoVenta.COMPLETADA, null, null, null, List.of());
    }

    private VentaAnulacion.Pendiente pendiente(EstadoVenta estado, String operacionId) {
        return new VentaAnulacion.Pendiente(5L, "B001-00000005", estado, operacionId,
                List.of(new ItemLote(1L, 2), new ItemLote(2L, 3)));
    }

    @Test
    @DisplayName("Anular: devuelve el stock por id de operación (cuenta de servicio) y recién después marca la venta")
    void anularDevuelveStockYMarca() {
        when(anulacion.preparar(5L)).thenReturn(pendiente(EstadoVenta.COMPLETADA, "op-aaaa-bbbb"));
        when(tokenServicio.generar()).thenReturn("token-servicio");
        VentaResponse anulada = respuesta();
        when(anulacion.marcarAnulada(5L, "Error de digitación", "admin")).thenReturn(anulada);

        VentaResponse resultado = service.anular(5L, "  Error de digitación ", "admin");

        assertThat(resultado).isSameAs(anulada);
        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(inventario, anulacion);
        orden.verify(inventario).revertirOperacion("op-aaaa-bbbb", "Anulación de venta B001-00000005", "token-servicio");
        orden.verify(anulacion).marcarAnulada(5L, "Error de digitación", "admin");
    }

    @Test
    @DisplayName("Anular una venta ya anulada: devuelve su estado y NO toca el inventario")
    void anularDosVeces() {
        when(anulacion.preparar(5L)).thenReturn(pendiente(EstadoVenta.ANULADA, "op-aaaa-bbbb"));
        VentaResponse existente = respuesta();
        when(anulacion.consultar(5L)).thenReturn(existente);

        assertThat(service.anular(5L, "otra vez", "admin")).isSameAs(existente);

        verifyNoInteractions(inventario);
        verify(anulacion, never()).marcarAnulada(any(), any(), any());
    }

    @Test
    @DisplayName("Anular una venta anterior al control de operaciones: devuelve por lista con clave fija por venta")
    void anularVentaAntigua() {
        when(anulacion.preparar(5L)).thenReturn(pendiente(EstadoVenta.COMPLETADA, null));
        when(tokenServicio.generar()).thenReturn("token-servicio");

        service.anular(5L, "Cliente se arrepintió", "admin");

        verify(inventario).revertir(List.of(new ItemLote(1L, 2), new ItemLote(2L, 3)),
                "Anulación de venta B001-00000005", "ANUL-00000005", "token-servicio");
        verify(anulacion).marcarAnulada(5L, "Cliente se arrepintió", "admin");
    }

    @Test
    @DisplayName("Anular con el inventario caído: la venta sigue COMPLETADA y se puede reintentar")
    void anularConInventarioCaido() {
        when(anulacion.preparar(5L)).thenReturn(pendiente(EstadoVenta.COMPLETADA, "op-aaaa-bbbb"));
        when(tokenServicio.generar()).thenReturn("token-servicio");
        doThrow(new ServicioNoDisponibleException("inventario caído"))
                .when(inventario).revertirOperacion(any(), any(), any());

        assertThatThrownBy(() -> service.anular(5L, "motivo", "admin"))
                .isInstanceOf(ServicioNoDisponibleException.class);

        verify(anulacion, never()).marcarAnulada(any(), any(), any());
    }

    @Test
    @DisplayName("Anular una venta inexistente: 404")
    void anularInexistente() {
        when(anulacion.preparar(99L)).thenThrow(new RecursoNoEncontradoException("No existe la venta con id 99"));

        assertThatThrownBy(() -> service.anular(99L, "motivo", "admin"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(inventario);
    }
}
