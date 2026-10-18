package com.mstock.ventas.exception;

/**
 * La petición al inventario salió pero no se supo el resultado (timeout o 5xx tras reintentar): puede que el
 * stock sí se haya descontado. Responde 503 como cualquier servicio caído, pero quien la captura debe
 * compensar por id de operación.
 */
public class ResultadoIncierto extends ServicioNoDisponibleException {

    public ResultadoIncierto(String mensaje) {
        super(mensaje);
    }
}
