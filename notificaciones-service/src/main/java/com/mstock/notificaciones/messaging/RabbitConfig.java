package com.mstock.notificaciones.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "mstock.eventos";
    public static final String EXCHANGE_DLX = "mstock.eventos.dlx";

    public static final String COLA_STOCK_BAJO = "notificaciones.stock-bajo";
    public static final String COLA_VENTAS = "notificaciones.venta-registrada";
    public static final String COLA_VENTAS_ANULADAS = "notificaciones.venta-anulada";
    public static final String COLA_DLQ = "notificaciones.dlq";

    public static final String RK_STOCK_BAJO = "stock.bajo";
    public static final String RK_VENTA_REGISTRADA = "venta.registrada";
    public static final String RK_VENTA_ANULADA = "venta.anulada";
    private static final String RK_DLQ = "notificaciones.dlq";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    Queue colaStockBajo() {
        return QueueBuilder.durable(COLA_STOCK_BAJO)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(RK_DLQ)
                .build();
    }

    @Bean
    Queue colaVentas() {
        return QueueBuilder.durable(COLA_VENTAS)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(RK_DLQ)
                .build();
    }

    @Bean
    Queue colaVentasAnuladas() {
        return QueueBuilder.durable(COLA_VENTAS_ANULADAS)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(RK_DLQ)
                .build();
    }

    @Bean
    Queue colaMuertos() {
        return QueueBuilder.durable(COLA_DLQ).build();
    }

    @Bean
    Binding bindingStockBajo(Queue colaStockBajo, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaStockBajo).to(eventosExchange).with(RK_STOCK_BAJO);
    }

    @Bean
    Binding bindingVentas(Queue colaVentas, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaVentas).to(eventosExchange).with(RK_VENTA_REGISTRADA);
    }

    @Bean
    Binding bindingVentasAnuladas(Queue colaVentasAnuladas, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaVentasAnuladas).to(eventosExchange).with(RK_VENTA_ANULADA);
    }

    @Bean
    Binding bindingMuertos(Queue colaMuertos, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(colaMuertos).to(deadLetterExchange).with(RK_DLQ);
    }

    /** Los eventos viajan como JSON. Spring Boot usa este conversor al enviar y al recibir. */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
