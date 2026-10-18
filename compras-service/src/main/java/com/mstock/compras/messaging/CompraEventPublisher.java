package com.mstock.compras.messaging;

import com.mstock.compras.event.CompraRegistradaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompraEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publicar(CompraRegistradaEvent evento) {
        Runnable envio = () -> {
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_EVENTOS,
                        RabbitConfig.RK_COMPRA_REGISTRADA, evento);
            } catch (RuntimeException e) {
                log.error("No se pudo publicar compra.registrada para compra {}. "
                        + "La compra quedó guardada, pero debe revisarse el evento: {}",
                        evento.compraId(), e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { envio.run(); }
            });
        } else {
            envio.run();
        }
    }
}
