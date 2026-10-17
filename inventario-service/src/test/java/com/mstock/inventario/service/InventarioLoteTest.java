package com.mstock.inventario.service;

import com.mstock.inventario.dto.ItemLote;
import com.mstock.inventario.dto.LoteRequest;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.entity.Movimiento;
import com.mstock.inventario.entity.Stock;
import com.mstock.inventario.entity.TipoMovimiento;
import com.mstock.inventario.exception.ReglaNegocioException;
import com.mstock.inventario.exception.StockInsuficienteException;
import com.mstock.inventario.repository.MovimientoRepository;
import com.mstock.inventario.repository.StockRepository;
import com.mstock.inventario.messaging.EventosPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioLoteTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private EventosPublisher eventos;
    @InjectMocks
    private InventarioService service;

    private Stock stock(long productoId, int cantidad) {
        return Stock.builder().productoId(productoId).cantidad(cantidad).build();
    }

    private void guardarMovimientosTalCual() {
        when(movimientoRepository.save(any(Movimiento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("Salida en lote: descuenta cada producto y registra un movimiento por producto")
    void salidaLoteDescuentaTodo() {
        Stock a = stock(1L, 10);
        Stock b = stock(2L, 5);
        when(stockRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));
        when(stockRepository.buscarParaActualizar(2L)).thenReturn(Optional.of(b));
        guardarMovimientosTalCual();

        List<MovimientoResponse> resultado = service.registrarSalidaLote(
                new LoteRequest(List.of(new ItemLote(1L, 3), new ItemLote(2L, 5)), "Venta"), "cajero");

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(m -> assertThat(m.tipo()).isEqualTo(TipoMovimiento.SALIDA));
        assertThat(a.getCantidad()).isEqualTo(7);
        assertThat(b.getCantidad()).isZero();
    }

    @Test
    @DisplayName("Salida en lote: une los productos repetidos y los procesa en orden de id")
    void salidaLoteUneRepetidosYOrdena() {
        Stock a = stock(1L, 10);
        Stock b = stock(2L, 10);
        when(stockRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));
        when(stockRepository.buscarParaActualizar(2L)).thenReturn(Optional.of(b));
        guardarMovimientosTalCual();

        List<MovimientoResponse> resultado = service.registrarSalidaLote(new LoteRequest(
                List.of(new ItemLote(2L, 1), new ItemLote(1L, 2), new ItemLote(2L, 3)), "Venta"), "cajero");

        assertThat(resultado).hasSize(2);
        assertThat(a.getCantidad()).isEqualTo(8);
        assertThat(b.getCantidad()).isEqualTo(6);
        InOrder orden = inOrder(stockRepository);
        orden.verify(stockRepository).buscarParaActualizar(1L);
        orden.verify(stockRepository).buscarParaActualizar(2L);
    }

    @Test
    @DisplayName("Salida en lote: si un producto no alcanza, lanza 409 (la transacción deshace el resto)")
    void salidaLoteConStockInsuficiente() {
        when(stockRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(stock(1L, 10)));
        when(stockRepository.buscarParaActualizar(2L)).thenReturn(Optional.of(stock(2L, 1)));
        guardarMovimientosTalCual();

        assertThatThrownBy(() -> service.registrarSalidaLote(
                new LoteRequest(List.of(new ItemLote(1L, 2), new ItemLote(2L, 5)), "Venta"), "cajero"))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("disponible 1, solicitado 5");
    }

    @Test
    @DisplayName("Salida en lote: rechaza cantidades repetidas que suman más del máximo")
    void salidaLoteRechazaSumaExcesiva() {
        assertThatThrownBy(() -> service.registrarSalidaLote(new LoteRequest(
                List.of(new ItemLote(1L, 600000), new ItemLote(1L, 600000)), "Venta"), "cajero"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Reversión: devuelve el stock de todos los productos como entradas")
    void reversionDevuelveElStock() {
        Stock a = stock(1L, 7);
        Stock b = stock(2L, 0);
        when(stockRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));
        when(stockRepository.buscarParaActualizar(2L)).thenReturn(Optional.of(b));
        guardarMovimientosTalCual();

        List<MovimientoResponse> resultado = service.registrarEntradaLote(
                new LoteRequest(List.of(new ItemLote(1L, 3), new ItemLote(2L, 5)), "Reversión de venta"),
                "cajero");

        assertThat(resultado).allSatisfy(m -> assertThat(m.tipo()).isEqualTo(TipoMovimiento.ENTRADA));
        assertThat(a.getCantidad()).isEqualTo(10);
        assertThat(b.getCantidad()).isEqualTo(5);
    }
}
