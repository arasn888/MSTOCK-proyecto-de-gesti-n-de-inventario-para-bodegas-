package com.mstock.ventas.service;

import com.mstock.ventas.client.ItemLote;
import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.entity.Venta;
import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.messaging.EventosPublisher;
import com.mstock.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pasos locales (base de datos de ventas) de la anulación, cada uno en su propia transacción corta.
 * Se separan de VentaService porque entre ambos pasos se llama al inventario y no se debe mantener
 * una transacción abierta mientras otro servicio responde.
 */
@Component
@RequiredArgsConstructor
public class VentaAnulacion {

    /** Lo que hace falta saber de la venta para devolver su stock. */
    public record Pendiente(Long id, String numero, EstadoVenta estado, String operacionId, List<ItemLote> items) {
    }

    private final VentaRepository ventaRepository;
    private final EventosPublisher eventos;

    @Transactional(readOnly = true)
    public Pendiente preparar(Long id) {
        Venta venta = ventaRepository.buscarConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id));
        List<ItemLote> items = venta.getDetalles().stream()
                .map(d -> new ItemLote(d.getProductoId(), d.getCantidad()))
                .toList();
        return new Pendiente(venta.getId(), venta.getNumero(), venta.getEstado(), venta.getOperacionId(), items);
    }

    /** Lectura con su propia transacción (el cliente de la venta se carga de forma diferida). */
    @Transactional(readOnly = true)
    public VentaResponse consultar(Long id) {
        return ventaRepository.buscarConDetalle(id)
                .map(VentaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id));
    }

    /**
     * Marca la venta como ANULADA si sigue COMPLETADA y avisa por evento. Si otra anulación se adelantó, no
     * hace nada más (no publica el evento otra vez). Devuelve la venta tal como quedó.
     */
    @Transactional
    public VentaResponse marcarAnulada(Long id, String motivo, String usuario) {
        int filas = ventaRepository.anularSiCompletada(id, motivo, usuario, LocalDateTime.now());
        Venta venta = ventaRepository.buscarConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id));
        if (filas == 1) {
            eventos.ventaAnulada(venta.getId(), venta.getNumero(), usuario, venta.getTotal(), motivo);
        }
        return VentaResponse.desde(venta);
    }
}
