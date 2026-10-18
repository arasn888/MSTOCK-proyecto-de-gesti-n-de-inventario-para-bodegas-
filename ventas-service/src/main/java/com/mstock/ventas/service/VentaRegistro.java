package com.mstock.ventas.service;

import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.entity.DetalleVenta;
import com.mstock.ventas.entity.Venta;
import com.mstock.ventas.messaging.EventosPublisher;
import com.mstock.ventas.repository.ClienteRepository;
import com.mstock.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Guarda la venta y su detalle en una sola transacción local. */
@Component
@RequiredArgsConstructor
public class VentaRegistro {

    static final String SERIE = "B001";

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final EventosPublisher eventos;

    @Transactional
    public VentaResponse guardar(Long clienteId, String usuario, String operacionId, List<LineaVenta> lineas) {
        Venta venta = Venta.builder()
                .usuario(usuario)
                .operacionId(operacionId)
                .total(BigDecimal.ZERO)
                .cliente(clienteId == null ? null : clienteRepository.getReferenceById(clienteId))
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (LineaVenta linea : lineas) {
            BigDecimal precio = linea.producto().precioVenta();
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(linea.cantidad()));
            venta.agregarDetalle(DetalleVenta.builder()
                    .productoId(linea.producto().id())
                    .codigo(linea.producto().codigo())
                    .nombre(linea.producto().nombre())
                    .cantidad(linea.cantidad())
                    .precioUnitario(precio)
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);
        }
        venta.setTotal(total);

        ventaRepository.saveAndFlush(venta);
        venta.setNumero(String.format("%s-%08d", SERIE, venta.getId()));
        ventaRepository.saveAndFlush(venta);
        // Se envía al confirmarse la transacción, no antes.
        eventos.ventaRegistrada(venta.getId(), usuario, total, lineas.size());
        return VentaResponse.desde(venta);
    }
}
