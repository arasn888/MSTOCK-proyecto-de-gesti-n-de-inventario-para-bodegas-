package com.mstock.inventario.messaging;

import com.mstock.inventario.dto.MovimientoRequest;
import com.mstock.inventario.messaging.CompraRegistradaListener.CompraRegistradaEvent;
import com.mstock.inventario.messaging.CompraRegistradaListener.ItemCompra;
import com.mstock.inventario.service.InventarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraRegistradaListenerTest {

    @Mock
    private InventarioService inventarioService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private EventosPublisher eventosPublisher;
    @InjectMocks
    private CompraRegistradaListener listener;

    private ItemCompra item(Long productoId, Integer cantidad) {
        return new ItemCompra(productoId, "Arroz", cantidad, new BigDecimal("2.50"));
    }

    @Test
    @DisplayName("Compra nueva: marca la compra como procesada y suma el stock con el usuario de la compra")
    void compraNueva() {
        when(jdbcTemplate.update(anyString(), eq(7L))).thenReturn(1);

        listener.consumir(new CompraRegistradaEvent(7L, 1L, "almacenero", List.of(item(1L, 5), item(2L, 3))));

        verify(inventarioService).registrarEntrada(
                new MovimientoRequest(1L, 5, "Compra #7 - Arroz"), "almacenero");
        verify(inventarioService).registrarEntrada(
                new MovimientoRequest(2L, 3, "Compra #7 - Arroz"), "almacenero");
        verify(eventosPublisher).compraAplicada(7L);
    }

    @Test
    @DisplayName("Sin usuario en el evento: el kardex queda a nombre de compras-service")
    void sinUsuario() {
        when(jdbcTemplate.update(anyString(), eq(7L))).thenReturn(1);

        listener.consumir(new CompraRegistradaEvent(7L, 1L, null, List.of(item(1L, 5))));

        verify(inventarioService).registrarEntrada(any(MovimientoRequest.class), eq("compras-service"));
    }

    @Test
    @DisplayName("Evento duplicado (RabbitMQ lo entregó dos veces): no se suma el stock otra vez")
    void eventoDuplicado() {
        when(jdbcTemplate.update(anyString(), eq(7L))).thenReturn(0);

        listener.consumir(new CompraRegistradaEvent(7L, 1L, "almacenero", List.of(item(1L, 5))));

        verifyNoInteractions(inventarioService);
        verify(eventosPublisher).compraAplicada(7L);
    }

    @Test
    @DisplayName("Evento sin productos: se rechaza la compra sin tocar la base de datos ni reintentar")
    void eventoSinItems() {
        listener.consumir(new CompraRegistradaEvent(7L, 1L, "u", List.of()));

        verify(eventosPublisher).compraRechazada(eq(7L), anyString());
        verifyNoInteractions(jdbcTemplate, inventarioService);
    }

    @Test
    @DisplayName("Evento sin compraId: no se puede responder a nadie, va a la cola de muertos")
    void eventoSinId() {
        assertThatThrownBy(() -> listener.consumir(new CompraRegistradaEvent(null, 1L, "u", List.of(item(1L, 1)))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbcTemplate, inventarioService, eventosPublisher);
    }

    @Test
    @DisplayName("Producto con cantidad inválida: se rechaza la compra completa sin sumar nada")
    void cantidadInvalida() {
        listener.consumir(new CompraRegistradaEvent(7L, 1L, "u", List.of(item(2L, 3), item(1L, 0))));

        verify(eventosPublisher).compraRechazada(eq(7L), anyString());
        verify(inventarioService, never()).registrarEntrada(any(), any());
        verifyNoInteractions(jdbcTemplate);
    }
}
