package com.mstock.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cierre de caja de un día: ventas por cajero según la fecha de la venta. Las anuladas no suman al total
 * vendido; se muestran aparte para que el cierre cuadre con lo que realmente quedó en caja.
 */
public record CierreCajaResponse(LocalDate fecha, List<Cajero> cajeros, Totales totales) {

    public record Cajero(String usuario, long cantidadVentas, BigDecimal totalVendido,
                         long cantidadAnuladas, BigDecimal totalAnulado) {
    }

    public record Totales(long cantidadVentas, BigDecimal totalVendido,
                          long cantidadAnuladas, BigDecimal totalAnulado) {
    }
}
