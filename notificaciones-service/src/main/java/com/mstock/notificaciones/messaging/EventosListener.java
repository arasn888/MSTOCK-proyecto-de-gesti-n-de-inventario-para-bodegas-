package com.mstock.notificaciones.messaging;

import com.mstock.notificaciones.event.StockBajoEvent;
import com.mstock.notificaciones.event.VentaAnuladaEvent;
import com.mstock.notificaciones.event.VentaRegistradaEvent;
import com.mstock.notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventosListener {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = RabbitConfig.COLA_STOCK_BAJO)
    public void stockBajo(StockBajoEvent evento) {
        notificacionService.registrarStockBajo(evento);
    }

    @RabbitListener(queues = RabbitConfig.COLA_VENTAS)
    public void ventaRegistrada(VentaRegistradaEvent evento) {
        notificacionService.registrarVenta(evento);
    }

    @RabbitListener(queues = RabbitConfig.COLA_VENTAS_ANULADAS)
    public void ventaAnulada(VentaAnuladaEvent evento) {
        notificacionService.registrarVentaAnulada(evento);
    }
}
