package com.mstock.inventario.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente declarativo del catálogo. "catalogo-service" se resuelve con Eureka; si se define
 * mstock.catalogo.url se llama directo a esa dirección (útil para probar sin Eureka).
 */
@FeignClient(name = "catalogo-service", url = "${mstock.catalogo.url:}", path = "/api/productos")
public interface ProductoCatalogoClient {

    @GetMapping("/{id}")
    ProductoCatalogo obtener(@PathVariable("id") Long id,
                             @RequestHeader("Authorization") String authorization);
}
