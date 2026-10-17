package com.mstock.ventas.service;

import com.mstock.ventas.client.ProductoCatalogo;

/** Un producto ya validado en el catálogo, con la cantidad a vender. */
public record LineaVenta(ProductoCatalogo producto, int cantidad) {
}
