package com.mstock.compras.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE_EVENTOS = "mstock.eventos";
    public static final String RK_COMPRA_REGISTRADA = "compra.registrada";
    public static final String RK_COMPRA_APLICADA = "compra.aplicada";
    public static final String RK_COMPRA_RECHAZADA = "compra.rechazada";

    public static final String COLA_RESULTADOS = "compras.resultado";
    public static final String EXCHANGE_DLX = "mstock.eventos.dlx";
    public static final String COLA_DLQ = "compras.dlq";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    Queue colaResultados() {
        return QueueBuilder.durable(COLA_RESULTADOS).build();
    }

    @Bean
    Binding bindingCompraAplicada(Queue colaResultados, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaResultados).to(eventosExchange).with(RK_COMPRA_APLICADA);
    }

    @Bean
    Binding bindingCompraRechazada(Queue colaResultados, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaResultados).to(eventosExchange).with(RK_COMPRA_RECHAZADA);
    }

    /** Mismo exchange de mensajes muertos que usan los otros servicios (declararlo es idempotente). */
    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    Queue colaMuertosCompras() {
        return QueueBuilder.durable(COLA_DLQ).build();
    }

    @Bean
    Binding bindingMuertosCompras(Queue colaMuertosCompras, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(colaMuertosCompras).to(deadLetterExchange).with(COLA_DLQ);
    }

    @Bean
    MessageRecoverer recuperadorDeFallos(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, EXCHANGE_DLX, COLA_DLQ);
    }
}
