package com.mstock.compras.messaging;

import com.mstock.compras.messaging.ResultadoCompraListener.CompraResultadoEvent;
import com.mstock.compras.service.ComprasService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ResultadoCompraListenerTest {

    @Mock
    private ComprasService comprasService;
    @InjectMocks
    private ResultadoCompraListener listener;

    @Test
    @DisplayName("compra.aplicada: la compra se marca como recibida")
    void aplicada() {
        listener.consumir(new CompraResultadoEvent(7L, null), RabbitConfig.RK_COMPRA_APLICADA);
        verify(comprasService).aplicarResultado(7L, true, null);
    }

    @Test
    @DisplayName("compra.rechazada: la compra se marca como rechazada con el motivo")
    void rechazada() {
        listener.consumir(new CompraResultadoEvent(7L, "sin productos"), RabbitConfig.RK_COMPRA_RECHAZADA);
        verify(comprasService).aplicarResultado(7L, false, "sin productos");
    }

    @Test
    @DisplayName("Evento sin compraId: va a la cola de muertos")
    void sinId() {
        assertThatThrownBy(() -> listener.consumir(new CompraResultadoEvent(null, null),
                RabbitConfig.RK_COMPRA_APLICADA)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(comprasService);
    }
}
