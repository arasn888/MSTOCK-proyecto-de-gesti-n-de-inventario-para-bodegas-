package com.mstock.ventas.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.mstock.ventas.event.VentaAnuladaEvent;
import com.mstock.ventas.event.VentaRegistradaEvent;

import java.math.BigDecimal;

/**
 * Publica eventos en RabbitMQ. Si hay una transacción en curso, el envío espera al commit
 * (un evento de algo que se revirtió sería mentira). Si RabbitMQ falla, solo se registra
 * en el log: la operación del usuario ya se hizo y no debe fallar por una notificación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventosPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void ventaRegistrada(Long ventaId, String usuario, BigDecimal total, int cantidadItems) {
        publicar(RabbitConfig.RK_VENTA_REGISTRADA, new VentaRegistradaEvent(ventaId, usuario, total, cantidadItems));
    }

    /** Usuario = quien anuló la venta. Se publica al confirmarse la transacción que la marca ANULADA. */
    public void ventaAnulada(Long ventaId, String numero, String usuario, BigDecimal total, String motivo) {
        publicar(RabbitConfig.RK_VENTA_ANULADA, new VentaAnuladaEvent(ventaId, numero, usuario, total, motivo));
    }

    private void publicar(String routingKey, Object evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(routingKey, evento);
                }
            });
        } else {
            enviar(routingKey, evento);
        }
    }

    private void enviar(String routingKey, Object evento) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS, routingKey, evento);
        } catch (RuntimeException e) {
            log.warn("No se pudo publicar el evento {} ({}): {}", routingKey, evento, e.getMessage());
        }
    }
}
