package com.mstock.inventario.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.mstock.inventario.event.CompraResultadoEvent;
import com.mstock.inventario.event.StockBajoEvent;

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

    /** El inventario no conoce el nombre del producto (es del catálogo): va null y el consumidor usa el id. */
    public void stockBajo(Long productoId, int cantidad, int stockMinimo) {
        publicar(RabbitConfig.RK_STOCK_BAJO, new StockBajoEvent(productoId, null, cantidad, stockMinimo));
    }

    /** El stock de la compra ya se sumó (se publica al confirmarse la transacción). */
    public void compraAplicada(Long compraId) {
        publicar(RabbitConfig.RK_COMPRA_APLICADA, new CompraResultadoEvent(compraId, null));
    }

    /** La compra no se pudo aplicar; compras-service la marca RECHAZADA con este motivo. */
    public void compraRechazada(Long compraId, String motivo) {
        publicar(RabbitConfig.RK_COMPRA_RECHAZADA, new CompraResultadoEvent(compraId, motivo));
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
