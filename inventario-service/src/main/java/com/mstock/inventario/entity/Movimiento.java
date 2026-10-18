package com.mstock.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private int stockAnterior;

    @Column(name = "stock_resultante", nullable = false)
    private int stockResultante;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false, length = 50)
    private String usuario;

    /** Id de la operación remota que originó el movimiento (idempotencia); null en los manuales. */
    @Column(name = "operacion_id", length = 64)
    private String operacionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
