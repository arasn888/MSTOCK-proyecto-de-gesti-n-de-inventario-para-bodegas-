package com.mstock.inventario.repository;

import com.mstock.inventario.entity.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByProductoIdOrderByFechaDescIdDesc(Long productoId);

    /** Movimientos de una operación remota, en el orden en que se aplicaron. */
    List<Movimiento> findByOperacionIdOrderByIdAsc(String operacionId);

    long countByProductoId(Long productoId);
}
