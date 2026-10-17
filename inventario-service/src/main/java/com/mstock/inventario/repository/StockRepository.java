package com.mstock.inventario.repository;

import com.mstock.inventario.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductoId(Long productoId);

    List<Stock> findAllByOrderByProductoIdAsc();

    /**
     * Bloquea la fila mientras dura la transacción: dos ventas simultáneas del
     * mismo producto no pueden leer el mismo stock y dejarlo en negativo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.productoId = :productoId")
    Optional<Stock> buscarParaActualizar(@Param("productoId") Long productoId);

    @Query("select s from Stock s where s.cantidad <= s.stockMinimo order by s.cantidad asc, s.productoId asc")
    List<Stock> findEnAlerta();
}
