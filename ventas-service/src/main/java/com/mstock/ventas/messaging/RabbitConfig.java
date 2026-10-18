package com.mstock.ventas.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTOS = "mstock.eventos";
    public static final String RK_STOCK_BAJO = "stock.bajo";
    public static final String RK_VENTA_REGISTRADA = "venta.registrada";
    public static final String RK_VENTA_ANULADA = "venta.anulada";

    /** Mismo exchange (topic, durable) que declara notificaciones-service: declararlo es idempotente. */
    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    /** Los eventos viajan como JSON; Spring Boot usa este conversor en el RabbitTemplate. */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
