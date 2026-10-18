package com.mstock.inventario.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mstock.inventario.dto.MovimientoRequest;
import com.mstock.inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompraRegistradaListener {
    private final InventarioService inventarioService;
    private final JdbcTemplate jdbcTemplate;
    private final EventosPublisher eventosPublisher;

    @Transactional
    @RabbitListener(queues = ComprasRabbitConfig.COLA_COMPRAS)
    public void consumir(CompraRegistradaEvent evento) {
        if (evento == null || evento.compraId() == null) {
            throw new IllegalArgumentException("Evento compra.registrada inválido");
        }
        // Un evento mal formado nunca se va a poder aplicar: se rechaza la compra de una vez, sin reintentos.
        if (evento.items() == null || evento.items().isEmpty()) {
            eventosPublisher.compraRechazada(evento.compraId(), "La compra no trae productos");
            return;
        }
        for (ItemCompra item : evento.items()) {
            if (item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) {
                eventosPublisher.compraRechazada(evento.compraId(), "La compra trae un producto o cantidad inválidos");
                return;
            }
        }

        int insertados = jdbcTemplate.update(
                "INSERT IGNORE INTO compras_procesadas (compra_id) VALUES (?)", evento.compraId());
        if (insertados == 0) {
            log.info("Se ignora evento duplicado de compra {}", evento.compraId());
            // Se vuelve a informar el resultado: si el aviso original se perdió, la compra no queda PENDIENTE.
            eventosPublisher.compraAplicada(evento.compraId());
            return;
        }

        // El kardex guarda quién hizo la compra; si el evento no lo trae, queda el servicio de origen.
        String usuario = (evento.usuario() == null || evento.usuario().isBlank())
                ? "compras-service" : evento.usuario().trim();

        for (ItemCompra item : evento.items()) {
            inventarioService.registrarEntrada(
                    new MovimientoRequest(item.productoId(), item.cantidad(),
                            "Compra #" + evento.compraId() + " - " + safe(item.productoNombre())),
                    usuario);
        }
        eventosPublisher.compraAplicada(evento.compraId());
        log.info("Compra {} aplicada al inventario ({} productos)", evento.compraId(), evento.items().size());
    }

    private String safe(String nombre) {
        return nombre == null ? "" : nombre;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompraRegistradaEvent(Long compraId, Long proveedorId, String usuario, List<ItemCompra> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemCompra(Long productoId, String productoNombre, Integer cantidad, BigDecimal costoUnitario) {
    }
}
