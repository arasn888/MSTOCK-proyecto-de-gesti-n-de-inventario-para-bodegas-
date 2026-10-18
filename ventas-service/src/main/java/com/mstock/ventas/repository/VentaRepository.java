package com.mstock.ventas.repository;

import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Cantidad y total de ventas de un estado en el rango. Con cero ventas: cantidad 0 y total null. */
    interface ResumenVentas {
        Long getCantidad();
        BigDecimal getTotal();
    }

    /** Cantidad y total por cajero y estado (el reporte de cierre de caja los junta). */
    interface TotalCajero {
        String getUsuario();
        EstadoVenta getEstado();
        Long getCantidad();
        BigDecimal getTotal();
    }

    @Query("select count(v) as cantidad, sum(v.total) as total from Venta v "
            + "where v.estado = :estado and v.fecha >= :desde and v.fecha < :hasta")
    ResumenVentas resumir(@Param("estado") EstadoVenta estado,
                          @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select v.usuario as usuario, v.estado as estado, count(v) as cantidad, sum(v.total) as total "
            + "from Venta v where v.fecha >= :desde and v.fecha < :hasta "
            + "group by v.usuario, v.estado order by v.usuario asc")
    List<TotalCajero> totalesPorCajero(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select distinct v from Venta v left join fetch v.detalles where v.id = :id")
    Optional<Venta> buscarConDetalle(@Param("id") Long id);

    /**
     * Pasa la venta a ANULADA solo si sigue COMPLETADA. Devuelve 1 si la anuló y 0 si ya estaba anulada
     * (otra anulación llegó antes): así dos anulaciones simultáneas no la procesan dos veces.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Venta v set v.estado = com.mstock.ventas.entity.EstadoVenta.ANULADA, "
            + "v.motivoAnulacion = :motivo, v.usuarioAnulacion = :usuario, v.fechaAnulacion = :fecha "
            + "where v.id = :id and v.estado = com.mstock.ventas.entity.EstadoVenta.COMPLETADA")
    int anularSiCompletada(@Param("id") Long id, @Param("motivo") String motivo,
                           @Param("usuario") String usuario, @Param("fecha") LocalDateTime fecha);

    /** Ventas con fecha desde (incluida) hasta (excluida), de la más reciente a la más antigua. */
    @Query("select v from Venta v where v.fecha >= :desde and v.fecha < :hasta order by v.fecha desc, v.id desc")
    List<Venta> buscarPorRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
