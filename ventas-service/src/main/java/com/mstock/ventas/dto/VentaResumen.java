package com.mstock.ventas.dto;

import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila de listado: sin el detalle, para no cargar las líneas de cada venta. */
public record VentaResumen(Long id, String numero, String clienteNombre, String usuario,
                           LocalDateTime fecha, BigDecimal total, EstadoVenta estado) {

    public static VentaResumen desde(Venta v) {
        return new VentaResumen(v.getId(), v.getNumero(),
                v.getCliente() != null ? v.getCliente().getNombres() : null,
                v.getUsuario(), v.getFecha(), v.getTotal(), v.getEstado());
    }
}
