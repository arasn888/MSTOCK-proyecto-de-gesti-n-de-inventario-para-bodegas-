package com.mstock.inventario.service;

import com.mstock.inventario.dto.AjusteRequest;
import com.mstock.inventario.dto.MovimientoRequest;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.dto.StockMinimoRequest;
import com.mstock.inventario.entity.Movimiento;
import com.mstock.inventario.entity.Stock;
import com.mstock.inventario.entity.TipoMovimiento;
import com.mstock.inventario.exception.RecursoNoEncontradoException;
import com.mstock.inventario.exception.ReglaNegocioException;
import com.mstock.inventario.exception.StockInsuficienteException;
import com.mstock.inventario.repository.MovimientoRepository;
import com.mstock.inventario.repository.StockRepository;
import com.mstock.inventario.messaging.EventosPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

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
    @DisplayName("Entrada: si el producto no tiene stock, lo crea y suma la cantidad")
    void entradaCreaElStockSiNoExiste() {
        when(stockRepository.buscarParaActualizar(7L)).thenReturn(Optional.empty());
        guardarMovimientosTalCual();

        MovimientoResponse respuesta = service.registrarEntrada(
                new MovimientoRequest(7L, 10, "Compra inicial"), "almacenero");

        assertThat(respuesta.tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(respuesta.stockAnterior()).isZero();
        assertThat(respuesta.stockResultante()).isEqualTo(10);
        assertThat(respuesta.usuario()).isEqualTo("almacenero");
        verify(stockRepository, atLeastOnce()).saveAndFlush(any(Stock.class));
    }

    @Test
    @DisplayName("Entrada: suma la cantidad al stock existente")
    void entradaSumaAlStockExistente() {
        when(stockRepository.buscarParaActualizar(7L)).thenReturn(Optional.of(stock(7L, 5)));
        guardarMovimientosTalCual();

        MovimientoResponse respuesta = service.registrarEntrada(
                new MovimientoRequest(7L, 10, null), "admin");

        assertThat(respuesta.stockAnterior()).isEqualTo(5);
        assertThat(respuesta.stockResultante()).isEqualTo(15);
        assertThat(respuesta.cantidad()).isEqualTo(10);
    }

    @Test
    @DisplayName("Entrada: no permite superar el stock máximo")
    void entradaQueSuperaElMaximo() {
        when(stockRepository.buscarParaActualizar(7L))
                .thenReturn(Optional.of(stock(7L, InventarioService.STOCK_MAXIMO - 5)));

        assertThatThrownBy(() -> service.registrarEntrada(new MovimientoRequest(7L, 10, null), "admin"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no puede superar");
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    @DisplayName("Salida: descuenta del stock y registra el movimiento")
    void salidaDescuentaElStock() {
        when(stockRepository.buscarParaActualizar(7L)).thenReturn(Optional.of(stock(7L, 20)));
        guardarMovimientosTalCual();

        MovimientoResponse respuesta = service.registrarSalida(
                new MovimientoRequest(7L, 8, "Venta"), "cajero");

        assertThat(respuesta.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(respuesta.stockAnterior()).isEqualTo(20);
        assertThat(respuesta.stockResultante()).isEqualTo(12);
    }

    @Test
    @DisplayName("Salida mayor al stock: lanza StockInsuficiente y no registra nada")
    void salidaMayorAlStock() {
        Stock existente = stock(7L, 4);
        when(stockRepository.buscarParaActualizar(7L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.registrarSalida(new MovimientoRequest(7L, 10, null), "cajero"))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("disponible 4")
                .hasMessageContaining("solicitado 10");

        assertThat(existente.getCantidad()).isEqualTo(4);
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    @DisplayName("Salida de un producto sin stock registrado: stock insuficiente (disponible 0)")
    void salidaSinRegistroDeStock() {
        when(stockRepository.buscarParaActualizar(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarSalida(new MovimientoRequest(7L, 1, null), "cajero"))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("disponible 0");
    }

    @Test
    @DisplayName("Ajuste: registra la diferencia y deja el stock en el valor real")
    void ajusteRegistraLaDiferencia() {
        when(stockRepository.buscarParaActualizar(7L)).thenReturn(Optional.of(stock(7L, 14)));
        guardarMovimientosTalCual();

        MovimientoResponse respuesta = service.registrarAjuste(
                new AjusteRequest(7L, 12, "Conteo físico"), "almacenero");

        assertThat(respuesta.tipo()).isEqualTo(TipoMovimiento.AJUSTE);
        assertThat(respuesta.cantidad()).isEqualTo(2);
        assertThat(respuesta.stockAnterior()).isEqualTo(14);
        assertThat(respuesta.stockResultante()).isEqualTo(12);
    }

    @Test
    @DisplayName("Ajuste igual al stock actual: no hay nada que ajustar")
    void ajusteIgualAlActual() {
        when(stockRepository.buscarParaActualizar(7L)).thenReturn(Optional.of(stock(7L, 12)));

        assertThatThrownBy(() -> service.registrarAjuste(new AjusteRequest(7L, 12, "Conteo"), "admin"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("nada que ajustar");
        verify(movimientoRepository, never()).save(any(Movimiento.class));
    }

    @Test
    @DisplayName("Stock mínimo de un producto sin inventario: no encontrado")
    void stockMinimoDeProductoSinInventario() {
        when(stockRepository.findByProductoId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarStockMinimo(7L, new StockMinimoRequest(10)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
