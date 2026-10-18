package com.mstock.compras.service;

import com.mstock.compras.client.CatalogoClient;
import com.mstock.compras.client.ProductoCatalogo;
import com.mstock.compras.client.ProveedorCatalogo;
import com.mstock.compras.dto.CompraRequest;
import com.mstock.compras.dto.CompraResponse;
import com.mstock.compras.dto.DetalleCompraRequest;
import com.mstock.compras.entity.Compra;
import com.mstock.compras.entity.EstadoCompra;
import com.mstock.compras.event.CompraRegistradaEvent;
import com.mstock.compras.messaging.CompraEventPublisher;
import com.mstock.compras.repository.CompraRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprasServiceTest {

    private static final String AUTH = "Bearer abc";

    @Mock
    private CompraRepository repository;
    @Mock
    private CatalogoClient catalogo;
    @Mock
    private CompraEventPublisher publisher;
    @InjectMocks
    private ComprasService service;

    private static FeignException error(int estado) {
        Request request = Request.create(Request.HttpMethod.GET, "http://catalogo-service/api/x",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response respuesta = Response.builder().status(estado).reason("error").request(request)
                .headers(Map.of()).build();
        return FeignException.errorStatus("CatalogoClient#x", respuesta);
    }

    private ProveedorCatalogo proveedor(long id, boolean activo) {
        return new ProveedorCatalogo(id, "20123456789", "Distribuidora SAC", null, null, null, activo);
    }

    private ProductoCatalogo producto(long id, String precioCompra, Long proveedorId, boolean activo) {
        return new ProductoCatalogo(id, "COD-" + id, "Producto " + id, null,
                new BigDecimal(precioCompra), new BigDecimal("9.99"), "UNIDAD", 1L, "Abarrotes",
                proveedorId, null, activo);
    }

    private CompraRequest pedido(Long proveedorId, DetalleCompraRequest... items) {
        return new CompraRequest(proveedorId, List.of(items));
    }

    private void guardarAsignandoId() {
        when(repository.saveAndFlush(any(Compra.class))).thenAnswer(invocacion -> {
            Compra c = invocacion.getArgument(0);
            c.setId(7L);
            return c;
        });
    }

    @Test
    @DisplayName("Compra correcta: costo del catálogo, total calculado y evento publicado")
    void compraCorrecta() {
        when(catalogo.obtenerProveedor(1L, AUTH)).thenReturn(proveedor(1, true));
        when(catalogo.obtenerProducto(10L, AUTH)).thenReturn(producto(10, "2.50", 1L, true));
        when(catalogo.obtenerProducto(11L, AUTH)).thenReturn(producto(11, "4.00", null, true));
        guardarAsignandoId();

        CompraResponse respuesta = service.registrar(
                pedido(1L, new DetalleCompraRequest(10L, 4), new DetalleCompraRequest(11L, 3)), "almacenero", "abc");

        assertThat(respuesta.total()).isEqualByComparingTo("22.00");
        assertThat(respuesta.items()).hasSize(2);
        assertThat(respuesta.usuario()).isEqualTo("almacenero");
        assertThat(respuesta.estado()).isEqualTo(EstadoCompra.PENDIENTE);
        ArgumentCaptor<CompraRegistradaEvent> evento = ArgumentCaptor.forClass(CompraRegistradaEvent.class);
        verify(publisher).publicar(evento.capture());
        assertThat(evento.getValue().compraId()).isEqualTo(7L);
        assertThat(evento.getValue().items()).hasSize(2);
        assertThat(evento.getValue().items().get(0).cantidad()).isEqualTo(4);
    }

    @Test
    @DisplayName("Proveedor inexistente: 404 y no se guarda ni se publica nada")
    void proveedorInexistente() {
        when(catalogo.obtenerProveedor(1L, AUTH)).thenThrow(error(404));

        assertThatThrownBy(() -> service.registrar(pedido(1L, new DetalleCompraRequest(10L, 1)), "u", "abc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("Proveedor inactivo: 409")
    void proveedorInactivo() {
        when(catalogo.obtenerProveedor(1L, AUTH)).thenReturn(proveedor(1, false));

        assertThatThrownBy(() -> service.registrar(pedido(1L, new DetalleCompraRequest(10L, 1)), "u", "abc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("Producto de otro proveedor: 409")
    void productoDeOtroProveedor() {
        when(catalogo.obtenerProveedor(1L, AUTH)).thenReturn(proveedor(1, true));
        when(catalogo.obtenerProducto(10L, AUTH)).thenReturn(producto(10, "2.50", 99L, true));

        assertThatThrownBy(() -> service.registrar(pedido(1L, new DetalleCompraRequest(10L, 1)), "u", "abc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("otro proveedor");
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("Producto inactivo: 409")
    void productoInactivo() {
        when(catalogo.obtenerProveedor(1L, AUTH)).thenReturn(proveedor(1, true));
        when(catalogo.obtenerProducto(10L, AUTH)).thenReturn(producto(10, "2.50", null, false));

        assertThatThrownBy(() -> service.registrar(pedido(1L, new DetalleCompraRequest(10L, 1)), "u", "abc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("Catálogo caído: 502 y no se guarda nada")
    void catalogoCaido() {
        when(catalogo.obtenerProveedor(1L, AUTH)).thenThrow(error(503));

        assertThatThrownBy(() -> service.registrar(pedido(1L, new DetalleCompraRequest(10L, 1)), "u", "abc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Obtener: una compra inexistente da 404")
    void obtenerInexistente() {
        when(repository.findById(9L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    private Compra pendiente() {
        Compra c = new Compra();
        c.setId(7L);
        return c;
    }

    @Test
    @DisplayName("Resultado aplicada: la compra pendiente pasa a RECIBIDA")
    void resultadoAplicada() {
        Compra c = pendiente();
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(c));

        service.aplicarResultado(7L, true, null);

        assertThat(c.getEstado()).isEqualTo(EstadoCompra.RECIBIDA);
        verify(repository).save(c);
    }

    @Test
    @DisplayName("Resultado rechazada: pasa a RECHAZADA y guarda el motivo")
    void resultadoRechazada() {
        Compra c = pendiente();
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(c));

        service.aplicarResultado(7L, false, "Producto inexistente");

        assertThat(c.getEstado()).isEqualTo(EstadoCompra.RECHAZADA);
        assertThat(c.getMotivoRechazo()).isEqualTo("Producto inexistente");
    }

    @Test
    @DisplayName("Resultado repetido: una compra que ya no está pendiente no cambia")
    void resultadoRepetido() {
        Compra c = pendiente();
        c.setEstado(EstadoCompra.RECIBIDA);
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(c));

        service.aplicarResultado(7L, false, "tarde");

        assertThat(c.getEstado()).isEqualTo(EstadoCompra.RECIBIDA);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Resultado de una compra inexistente: se ignora sin fallar")
    void resultadoCompraInexistente() {
        when(repository.findById(99L)).thenReturn(java.util.Optional.empty());

        service.aplicarResultado(99L, true, null);

        verify(repository, never()).save(any());
    }
}
