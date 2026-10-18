package com.mstock.ventas.client;

/** Fallo del inventario (caído, lento o 5xx). Cuenta para el circuito; el gateway lo reintenta solo con operaciones idempotentes. */
public class FalloInventarioException extends RuntimeException {

    public FalloInventarioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
