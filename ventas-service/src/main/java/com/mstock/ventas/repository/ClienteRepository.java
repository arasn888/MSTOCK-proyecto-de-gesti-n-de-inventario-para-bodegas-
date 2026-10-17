package com.mstock.ventas.repository;

import com.mstock.ventas.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    List<Cliente> findByActivoTrueOrderByNombresAsc();

    List<Cliente> findByActivoTrueAndNombresContainingIgnoreCaseOrderByNombresAsc(String nombres);
}
