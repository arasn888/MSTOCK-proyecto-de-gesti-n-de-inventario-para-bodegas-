package com.mstock.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    public static final int STOCK_MINIMO_POR_DEFECTO = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false, unique = true)
    private Long productoId;

    @Builder.Default
    @Column(nullable = false)
    private int cantidad = 0;

    @Builder.Default
    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo = STOCK_MINIMO_POR_DEFECTO;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void marcarFecha() {
        fechaActualizacion = LocalDateTime.now();
    }

    public boolean bajoMinimo() {
        return cantidad <= stockMinimo;
    }
}
