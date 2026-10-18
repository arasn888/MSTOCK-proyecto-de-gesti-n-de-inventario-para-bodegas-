package com.mstock.inventario.service;

import com.mstock.inventario.dto.ItemLote;
import com.mstock.inventario.dto.LoteRequest;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.entity.Movimiento;
import com.mstock.inventario.entity.Stock;
import com.mstock.inventario.entity.TipoMovimiento;
import com.mstock.inventario.exception.ReglaNegocioException;
import com.mstock.inventario.messaging.EventosPublisher;
import com.mstock.inventario.repository.MovimientoRepository;
import com.mstock.inventario.repository.Operaciones;
import com.mstock.inventario.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioIdempotenciaTest {

    private static final String OP = "7f3c1a52-0d1e-4c8a-9a55-1b2c3d4e5f60";

    @Mock
    private StockRepository stockRepository;
    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private EventosPublisher eventos;
    @Mock
    private Operaciones operaciones;
    @InjectMocks
    private InventarioService service;

    private Stock stock(long productoId, int cantidad) {
        return Stock.builder().productoId(productoId).cantidad(cantidad).build();
    }

    private Movimiento movimiento(long productoId, TipoMovimiento tipo, int cantidad, String operacionId) {
        return Movimiento.builder().productoId(productoId).tipo(tipo).cantidad(cantidad)
                .stockAnterior(10).stockResultante(10 - cantidad).usuario("cajero").operacionId(operacionId).build();
    }

    private LoteRequest lote(String operacionId) {
        return new LoteRequest(List.of(new ItemLote(1L, 3)), "Venta de mostrador", operacionId);
    }

    @Test
    @DisplayName("Descuento nuevo con operacionId: descuenta y guarda el id en el movimiento")
    void descuentoNuevo() {
        Stock a = stock(1L, 10);
        when(operaciones.existe("REV:" + OP)).thenReturn(false);
        when(operaciones.registrar(OP)).thenReturn(true);
        when(stockRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        List<MovimientoResponse> resultado = service.registrarSalidaLote(lote(OP), "cajero");

        assertThat(resultado).hasSize(1);
        assertThat(a.getCantidad()).isEqualTo(7);
        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        assertThat(captor.getValue().getOperacionId()).isEqualTo(OP);
    }

    @Test
    @DisplayName("Mismo operacionId otra vez (timeout y reintento): no descuenta de nuevo y devuelve lo de la primera vez")
    void reintentoNoDescuentaDosVeces() {
        when(operaciones.existe("REV:" + OP)).thenReturn(false);
        when(operaciones.registrar(OP)).thenReturn(false);
        when(movimientoRepository.findByOperacionIdOrderByIdAsc(OP))
                .thenReturn(List.of(movimiento(1L, TipoMovimiento.SALIDA, 3, OP)));

        List<MovimientoResponse> resultado = service.registrarSalidaLote(lote(OP), "cajero");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).cantidad()).isEqualTo(3);
        verify(stockRepository, never()).buscarParaActualizar(any());
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    @DisplayName("Descuento tardío de una operación ya cancelada: se rechaza")
    void descuentoTardioTrasReversion() {
        when(operaciones.existe("REV:" + OP)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarSalidaLote(lote(OP), "cajero"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cancelada");
        verify(operaciones, never()).registrar(any());
        verify(stockRepository, never()).buscarParaActualizar(any());
    }

    @Test
    @DisplayName("Sin operacionId el lote se comporta como antes (no consulta operaciones)")
    void sinOperacionId() {
        Stock a = stock(1L, 10);
        when(stockRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        service.registrarSalidaLote(lote(null), "cajero");

        assertThat(a.getCantidad()).isEqualTo(7);
        verify(operaciones, never()).registrar(any());
        verify(operaciones, never()).existe(any());
    }

    @Test
    @DisplayName("Reversión por operación: devuelve exactamente lo descontado y marca la reversión")
    void reversionDevuelveLoDescontado() {
        Stock a = stock(1L, 7);
        Stock b = stock(2L, 0);
        when(operaciones.registrar("REV:" + OP)).thenReturn(true);
        when(movimientoRepository.findByOperacionIdOrderByIdAsc(OP)).thenReturn(List.of(
                movimiento(2L, TipoMovimiento.SALIDA, 5, OP),
                movimiento(1L, TipoMovimiento.SALIDA, 3, OP)));
        when(stockRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));
        when(stockRepository.buscarParaActualizar(2L)).thenReturn(Optional.of(b));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        List<MovimientoResponse> resultado = service.revertirOperacion(OP, "Anulación de venta B001-1", "admin");

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(m -> assertThat(m.tipo()).isEqualTo(TipoMovimiento.ENTRADA));
        assertThat(a.getCantidad()).isEqualTo(10);
        assertThat(b.getCantidad()).isEqualTo(5);
        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(m -> assertThat(m.getOperacionId()).isEqualTo("REV:" + OP));
    }

    @Test
    @DisplayName("Reversión repetida: no devuelve el stock otra vez")
    void reversionRepetida() {
        when(operaciones.registrar("REV:" + OP)).thenReturn(false);
        when(movimientoRepository.findByOperacionIdOrderByIdAsc("REV:" + OP))
                .thenReturn(List.of(movimiento(1L, TipoMovimiento.ENTRADA, 3, "REV:" + OP)));

        List<MovimientoResponse> resultado = service.revertirOperacion(OP, "Anulación", "admin");

        assertThat(resultado).hasSize(1);
        verify(stockRepository, never()).buscarParaActualizar(any());
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    @DisplayName("Reversión de una operación que nunca llegó a aplicarse: no devuelve nada (no infla el stock)")
    void reversionDeAlgoQueNoSeAplico() {
        when(operaciones.registrar("REV:" + OP)).thenReturn(true);
        when(movimientoRepository.findByOperacionIdOrderByIdAsc(OP)).thenReturn(List.of());

        List<MovimientoResponse> resultado = service.revertirOperacion(OP, "Timeout", "ventas-service");

        assertThat(resultado).isEmpty();
        verify(stockRepository, never()).buscarParaActualizar(any());
    }
}
