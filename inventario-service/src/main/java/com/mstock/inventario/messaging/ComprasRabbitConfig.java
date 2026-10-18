package com.mstock.inventario.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class ComprasRabbitConfig {
    public static final String COLA_COMPRAS = "inventario.compra-registrada";
    public static final String RK_COMPRA_REGISTRADA = "compra.registrada";

    /** Mismo exchange de mensajes muertos que declara notificaciones-service (declararlo es idempotente). */
    public static final String EXCHANGE_DLX = "mstock.eventos.dlx";
    public static final String COLA_DLQ = "inventario.dlq";

    @Bean
    Queue colaComprasRegistradas() {
        return QueueBuilder.durable(COLA_COMPRAS).build();
    }

    @Bean
    Binding bindingComprasRegistradas(Queue colaComprasRegistradas,
                                      TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaComprasRegistradas)
                .to(eventosExchange).with(RK_COMPRA_REGISTRADA);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    Queue colaMuertosInventario() {
        return QueueBuilder.durable(COLA_DLQ).build();
    }

    @Bean
    Binding bindingMuertosInventario(Queue colaMuertosInventario, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(colaMuertosInventario).to(deadLetterExchange).with(COLA_DLQ);
    }

    /**
     * Una compra que falla 3 veces (ver spring.rabbitmq.listener.simple.retry en application.yml)
     * se manda a inventario.dlq en vez de reintentarse para siempre. No se cambian los argumentos de
     * la cola original, así que no hace falta borrarla en RabbitMQ.
     */
    @Bean
    MessageRecoverer recuperadorDeFallos(RabbitTemplate rabbitTemplate, EventosPublisher eventosPublisher) {
        MessageRecoverer aDlq = new RepublishMessageRecoverer(rabbitTemplate, EXCHANGE_DLX, COLA_DLQ);
        return (mensaje, causa) -> {
            // Se agotaron los reintentos: la transacción se revirtió, así que el stock no se tocó.
            // Se avisa a compras-service para que la compra no quede PENDIENTE para siempre.
            Long compraId = extraerCompraId(mensaje);
            if (compraId != null) {
                eventosPublisher.compraRechazada(compraId, "No se pudo aplicar al inventario: "
                        + (causa.getCause() != null ? causa.getCause() : causa).getMessage());
            }
            aDlq.recover(mensaje, causa);
        };
    }

    private static final Pattern COMPRA_ID = Pattern.compile("\"compraId\"\\s*:\\s*(\\d+)");

    static Long extraerCompraId(Message mensaje) {
        try {
            Matcher m = COMPRA_ID.matcher(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            return m.find() ? Long.valueOf(m.group(1)) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
