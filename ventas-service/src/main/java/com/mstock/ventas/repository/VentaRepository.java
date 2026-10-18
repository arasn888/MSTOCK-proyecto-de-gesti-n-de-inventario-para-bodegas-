package com.mstock.ventas.repository;

import com.mstock.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

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
