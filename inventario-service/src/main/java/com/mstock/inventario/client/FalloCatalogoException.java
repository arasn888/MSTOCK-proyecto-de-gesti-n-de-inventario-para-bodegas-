package com.mstock.inventario.client;

/** Fallo transitorio del catálogo (caído, lento o 5xx). Se reintenta y cuenta para el circuito. */
public class FalloCatalogoException extends RuntimeException {

    public FalloCatalogoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
