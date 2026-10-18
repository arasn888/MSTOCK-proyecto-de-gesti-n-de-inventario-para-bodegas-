package com.mstock.ventas.dto;

import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(Long id, String numero, Long clienteId, String clienteNombre, String usuario,
                            LocalDateTime fecha, BigDecimal total, EstadoVenta estado,
                            String motivoAnulacion, LocalDateTime fechaAnulacion, String usuarioAnulacion,
                            List<DetalleResponse> detalle) {

    public static VentaResponse desde(Venta v) {
        boolean conCliente = v.getCliente() != null;
        return new VentaResponse(v.getId(), v.getNumero(),
                conCliente ? v.getCliente().getId() : null,
                conCliente ? v.getCliente().getNombres() : null,
                v.getUsuario(), v.getFecha(), v.getTotal(), v.getEstado(),
                v.getMotivoAnulacion(), v.getFechaAnulacion(), v.getUsuarioAnulacion(),
                v.getDetalles().stream().map(DetalleResponse::desde).toList());
    }
}
