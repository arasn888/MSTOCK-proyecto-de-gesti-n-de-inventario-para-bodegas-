package com.mstock.ventas.exception;

/** El inventario no tiene unidades suficientes. El mensaje es el que devolvió el inventario. */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
