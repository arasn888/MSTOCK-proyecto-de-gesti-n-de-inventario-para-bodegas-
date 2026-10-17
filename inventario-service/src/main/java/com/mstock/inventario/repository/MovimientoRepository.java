package com.mstock.inventario.repository;

import com.mstock.inventario.entity.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByProductoIdOrderByFechaDescIdDesc(Long productoId);

    long countByProductoId(Long productoId);
}
