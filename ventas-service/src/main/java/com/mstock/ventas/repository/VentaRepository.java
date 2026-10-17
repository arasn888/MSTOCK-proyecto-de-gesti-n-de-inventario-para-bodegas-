package com.mstock.ventas.repository;

import com.mstock.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("select distinct v from Venta v left join fetch v.detalles where v.id = :id")
    Optional<Venta> buscarConDetalle(@Param("id") Long id);

    /** Ventas con fecha desde (incluida) hasta (excluida), de la más reciente a la más antigua. */
    @Query("select v from Venta v where v.fecha >= :desde and v.fecha < :hasta order by v.fecha desc, v.id desc")
    List<Venta> buscarPorRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
