package com.mstock.compras.entity;

/**
 * PENDIENTE: la compra está guardada y el inventario aún no confirmó el ingreso de stock.
 * RECIBIDA: el inventario sumó el stock. RECHAZADA: el inventario no pudo aplicarla (ver motivoRechazo).
 */
public enum EstadoCompra {
    PENDIENTE, RECIBIDA, RECHAZADA
}
