package com.mstock.inventario.client;

/** Lo que el inventario necesita saber de un producto del catálogo: que existe y si está activo. */
public record ProductoCatalogo(Long id, String codigo, String nombre, boolean activo) {
}
