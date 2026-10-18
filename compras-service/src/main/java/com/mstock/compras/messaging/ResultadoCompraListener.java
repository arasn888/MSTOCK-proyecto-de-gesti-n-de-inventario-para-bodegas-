package com.mstock.compras.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mstock.compras.service.ComprasService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** Recibe de inventario si la compra se aplicó (compra.aplicada) o no (compra.rechazada). */
@Component
@RequiredArgsConstructor
public class ResultadoCompraListener {
    private final ComprasService comprasService;

    @RabbitListener(queues = RabbitConfig.COLA_RESULTADOS)
    public void consumir(CompraResultadoEvent evento,
                         @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        if (evento == null || evento.compraId() == null) {
            throw new IllegalArgumentException("Evento de resultado de compra inválido");
        }
        comprasService.aplicarResultado(evento.compraId(),
                RabbitConfig.RK_COMPRA_APLICADA.equals(routingKey), evento.motivo());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompraResultadoEvent(Long compraId, String motivo) {
    }
}
