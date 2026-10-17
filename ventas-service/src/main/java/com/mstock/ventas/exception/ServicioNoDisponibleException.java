package com.mstock.ventas.exception;

/** Un servicio del que depende esta operación no responde: se responde 503. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
