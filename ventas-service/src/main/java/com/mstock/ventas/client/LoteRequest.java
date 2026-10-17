package com.mstock.ventas.client;

import java.util.List;

public record LoteRequest(List<ItemLote> items, String motivo) {
}
