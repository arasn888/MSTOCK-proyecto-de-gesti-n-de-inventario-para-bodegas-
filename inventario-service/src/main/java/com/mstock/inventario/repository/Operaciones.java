package com.mstock.inventario.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Registro de operaciones ya aplicadas (tabla operaciones). El INSERT IGNORE sobre la clave primaria
 * decide quién gana si dos peticiones con el mismo id llegan a la vez: la segunda espera al commit
 * de la primera y recibe 0 filas insertadas.
 */
@Component
@RequiredArgsConstructor
public class Operaciones {

    public static final String PREFIJO_REVERSION = "REV:";

    private final JdbcTemplate jdbcTemplate;

    /** @return true si el id era nuevo (la operación debe aplicarse); false si ya estaba registrado. */
    public boolean registrar(String operacionId) {
        return jdbcTemplate.update("INSERT IGNORE INTO operaciones (operacion_id) VALUES (?)", operacionId) == 1;
    }

    public boolean existe(String operacionId) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operaciones WHERE operacion_id = ?", Integer.class, operacionId);
        return total != null && total > 0;
    }
}
