package com.mstock.notificaciones.service;

import com.mstock.notificaciones.dto.LecturaMasivaResponse;
import com.mstock.notificaciones.dto.NotificacionResponse;
import com.mstock.notificaciones.dto.ResumenResponse;
import com.mstock.notificaciones.entity.Notificacion;
import com.mstock.notificaciones.entity.TipoNotificacion;
import com.mstock.notificaciones.event.StockBajoEvent;
import com.mstock.notificaciones.event.VentaAnuladaEvent;
import com.mstock.notificaciones.event.VentaRegistradaEvent;
import com.mstock.notificaciones.exception.RecursoNoEncontradoException;
import com.mstock.notificaciones.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repository;

    /**
     * Crea la alerta de stock bajo. Si el producto ya tiene una alerta sin leer,
     * no crea otra: así una racha de ventas no llena la bandeja de avisos repetidos.
     */
    @Transactional
    public void registrarStockBajo(StockBajoEvent evento) {
        if (evento.productoId() == null) {
            throw new IllegalArgumentException("El evento de stock bajo no trae productoId");
        }
        if (repository.existsByTipoAndReferenciaIdAndLeidaFalse(
                TipoNotificacion.STOCK_BAJO, evento.productoId())) {
            log.info("Ya existe una alerta sin leer para el producto {}; no se duplica", evento.productoId());
            return;
        }
        String producto = (evento.productoNombre() == null || evento.productoNombre().isBlank())
                ? "#" + evento.productoId()
                : evento.productoNombre().trim() + " (#" + evento.productoId() + ")";

        repository.save(Notificacion.builder()
                .tipo(TipoNotificacion.STOCK_BAJO)
                .titulo("Stock bajo")
                .mensaje("El producto " + producto + " tiene " + evento.cantidad()
                        + " unidad(es); el mínimo es " + evento.stockMinimo())
                .referenciaId(evento.productoId())
                .build());
    }

    @Transactional
    public void registrarVenta(VentaRegistradaEvent evento) {
        if (evento.ventaId() == null) {
            throw new IllegalArgumentException("El evento de venta no trae ventaId");
        }
        BigDecimal total = evento.total() == null ? BigDecimal.ZERO : evento.total();
        String usuario = (evento.usuario() == null || evento.usuario().isBlank())
                ? "un usuario" : evento.usuario().trim();

        repository.save(Notificacion.builder()
                .tipo(TipoNotificacion.VENTA_REGISTRADA)
                .titulo("Nueva venta registrada")
                .mensaje("Venta #" + evento.ventaId() + " por S/ "
                        + total.setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + " registrada por " + usuario)
                .referenciaId(evento.ventaId())
                .build());
    }

    @Transactional
    public void registrarVentaAnulada(VentaAnuladaEvent evento) {
        if (evento.ventaId() == null) {
            throw new IllegalArgumentException("El evento de venta anulada no trae ventaId");
        }
        BigDecimal total = evento.total() == null ? BigDecimal.ZERO : evento.total();
        String usuario = (evento.usuario() == null || evento.usuario().isBlank())
                ? "un administrador" : evento.usuario().trim();
        String venta = (evento.numero() == null || evento.numero().isBlank())
                ? "#" + evento.ventaId() : evento.numero().trim();
        String motivo = (evento.motivo() == null || evento.motivo().isBlank())
                ? "" : ". Motivo: " + evento.motivo().trim();

        String mensaje = "Venta " + venta + " por S/ " + total.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + " anulada por " + usuario + motivo;
        repository.save(Notificacion.builder()
                .tipo(TipoNotificacion.VENTA_ANULADA)
                .titulo("Venta anulada")
                // La columna admite 500 caracteres; el motivo del usuario puede ser largo.
                .mensaje(mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje)
                .referenciaId(evento.ventaId())
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(boolean soloNoLeidas) {
        List<Notificacion> lista = soloNoLeidas
                ? repository.findByLeidaFalseOrderByFechaDescIdDesc()
                : repository.findAllByOrderByFechaDescIdDesc();
        return lista.stream().map(NotificacionResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen() {
        return new ResumenResponse(repository.countByLeidaFalse());
    }

    @Transactional
    public NotificacionResponse marcarLeida(Long id) {
        Notificacion notificacion = buscar(id);
        notificacion.setLeida(true);
        return NotificacionResponse.desde(notificacion);
    }

    @Transactional
    public LecturaMasivaResponse marcarTodasLeidas() {
        return new LecturaMasivaResponse(repository.marcarTodasLeidas());
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe la notificación con id " + id);
        }
        repository.deleteById(id);
    }

    private Notificacion buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la notificación con id " + id));
    }
}
