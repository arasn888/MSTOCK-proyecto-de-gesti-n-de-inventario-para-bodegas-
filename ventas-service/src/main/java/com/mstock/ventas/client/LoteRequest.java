package com.mstock.ventas.client;

import java.util.List;

/** operacionId (opcional) vuelve idempotente la operación en el inventario: el mismo id se aplica una sola vez. */
public record LoteRequest(List<ItemLote> items, String motivo, String operacionId) {

    public LoteRequest(List<ItemLote> items, String motivo) {
        this(items, motivo, null);
    }
}
