package com.mstock.inventario.exception;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(int disponible, int solicitado) {
        super("Stock insuficiente: disponible " + disponible + ", solicitado " + solicitado);
    }
}
