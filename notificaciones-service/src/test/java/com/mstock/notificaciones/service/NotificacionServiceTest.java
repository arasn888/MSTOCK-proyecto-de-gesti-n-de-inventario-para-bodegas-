package com.mstock.notificaciones.service;

import com.mstock.notificaciones.dto.NotificacionResponse;
import com.mstock.notificaciones.entity.Notificacion;
import com.mstock.notificaciones.entity.TipoNotificacion;
import com.mstock.notificaciones.event.StockBajoEvent;
import com.mstock.notificaciones.event.VentaRegistradaEvent;
import com.mstock.notificaciones.exception.RecursoNoEncontradoException;
import com.mstock.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repository;
    @InjectMocks
    private NotificacionService service;

    @Test
    @DisplayName("Stock bajo: crea la alerta con el nombre y las cantidades")
    void stockBajoCreaLaAlerta() {
        when(repository.existsByTipoAndReferenciaIdAndLeidaFalse(TipoNotificacion.STOCK_BAJO, 7L))
                .thenReturn(false);

        service.registrarStockBajo(new StockBajoEvent(7L, "Gaseosa 500 ml", 3, 5));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        Notificacion creada = captor.getValue();
        assertThat(creada.getTipo()).isEqualTo(TipoNotificacion.STOCK_BAJO);
        assertThat(creada.getReferenciaId()).isEqualTo(7L);
        assertThat(creada.isLeida()).isFalse();
        assertThat(creada.getMensaje())
                .contains("Gaseosa 500 ml")
                .contains("3 unidad(es)")
                .contains("mínimo es 5");
    }

    @Test
    @DisplayName("Stock bajo repetido: no duplica la alerta si ya hay una sin leer")
    void stockBajoNoDuplica() {
        when(repository.existsByTipoAndReferenciaIdAndLeidaFalse(TipoNotificacion.STOCK_BAJO, 7L))
                .thenReturn(true);

        service.registrarStockBajo(new StockBajoEvent(7L, "Gaseosa 500 ml", 2, 5));

        verify(repository, never()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("Stock bajo sin productoId: el evento es inválido")
    void stockBajoSinProducto() {
        assertThatThrownBy(() -> service.registrarStockBajo(new StockBajoEvent(null, "X", 1, 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productoId");
        verify(repository, never()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("Venta registrada: arma el mensaje con el total a dos decimales")
    void ventaRegistrada() {
        service.registrarVenta(new VentaRegistradaEvent(15L, "cajero1", new BigDecimal("25.5"), 3));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        Notificacion creada = captor.getValue();
        assertThat(creada.getTipo()).isEqualTo(TipoNotificacion.VENTA_REGISTRADA);
        assertThat(creada.getReferenciaId()).isEqualTo(15L);
        assertThat(creada.getMensaje())
                .contains("Venta #15")
                .contains("S/ 25.50")
                .contains("cajero1");
    }

    @Test
    @DisplayName("Marcar como leída: cambia el estado de la notificación")
    void marcarLeida() {
        Notificacion pendiente = Notificacion.builder()
                .id(3L).tipo(TipoNotificacion.STOCK_BAJO).titulo("t").mensaje("m").build();
        when(repository.findById(3L)).thenReturn(Optional.of(pendiente));

        NotificacionResponse respuesta = service.marcarLeida(3L);

        assertThat(respuesta.leida()).isTrue();
        assertThat(pendiente.isLeida()).isTrue();
    }

    @Test
    @DisplayName("Marcar como leída una notificación inexistente: no encontrada")
    void marcarLeidaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Eliminar una notificación inexistente: no encontrada y no borra nada")
    void eliminarInexistente() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repository, never()).deleteById(any());
    }
}
