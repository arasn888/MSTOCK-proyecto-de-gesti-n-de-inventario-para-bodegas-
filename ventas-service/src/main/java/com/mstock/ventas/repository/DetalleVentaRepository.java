package com.mstock.ventas.repository;

import com.mstock.ventas.entity.DetalleVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Consultas de reportes sobre las líneas de venta. Solo cuentan las ventas COMPLETADAS. */
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    interface ProductoVendido {
        Long getProductoId();
        String getCodigo();
        String getNombre();
        Long getCantidad();
        BigDecimal getIngresos();
    }

    interface Rentabilidad {
        /** Suma de (precio - costo) x cantidad de las líneas que tienen costo; null si ninguna lo tiene. */
        BigDecimal getGanancia();
        /** Líneas sin costo (ventas antiguas): no entran en la ganancia. Null si no hay líneas. */
        Long getLineasSinCosto();
    }

    @Query("select d.productoId as productoId, max(d.codigo) as codigo, max(d.nombre) as nombre, "
            + "sum(d.cantidad) as cantidad, sum(d.subtotal) as ingresos "
            + "from DetalleVenta d join d.venta v "
            + "where v.estado = com.mstock.ventas.entity.EstadoVenta.COMPLETADA "
            + "and v.fecha >= :desde and v.fecha < :hasta "
            + "group by d.productoId "
            + "order by sum(d.cantidad) desc, d.productoId asc")
    List<ProductoVendido> masVendidos(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                      Pageable limite);

    @Query("select sum((d.precioUnitario - d.costoUnitario) * d.cantidad) as ganancia, "
            + "sum(case when d.costoUnitario is null then 1 else 0 end) as lineasSinCosto "
            + "from DetalleVenta d join d.venta v "
            + "where v.estado = com.mstock.ventas.entity.EstadoVenta.COMPLETADA "
            + "and v.fecha >= :desde and v.fecha < :hasta")
    Rentabilidad rentabilidad(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
