package com.mstock.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de ventas de un rango de fechas (por defecto, hoy). Solo las ventas COMPLETADAS cuentan en
 * {@code ventas}, {@code masVendidos} y la ganancia; las anuladas se informan aparte.
 * El stock crítico no va aquí: es del inventario (GET /api/inventario/alertas).
 */
public record DashboardResponse(LocalDate desde, LocalDate hasta, Ventas ventas, Anuladas anuladas,
                                List<ProductoMasVendido> masVendidos) {

    /**
     * ganancia: suma de (precio - costo) x cantidad. lineasSinCosto: líneas de ventas antiguas sin costo
     * registrado, que no entran en la ganancia (si es mayor que 0, la ganancia está subestimada).
     */
    public record Ventas(long cantidad, BigDecimal total, BigDecimal ticketPromedio,
                         BigDecimal ganancia, long lineasSinCosto) {
    }

    public record Anuladas(long cantidad, BigDecimal total) {
    }

    public record ProductoMasVendido(Long productoId, String codigo, String nombre,
                                     long unidadesVendidas, BigDecimal ingresos) {
    }
}
