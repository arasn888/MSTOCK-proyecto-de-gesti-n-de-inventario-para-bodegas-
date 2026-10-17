package com.mstock.ventas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalogo-service", url = "${mstock.catalogo.url:}", path = "/api/productos")
public interface ProductoCatalogoClient {

    @GetMapping("/{id}")
    ProductoCatalogo obtener(@PathVariable("id") Long id,
                             @RequestHeader("Authorization") String authorization);
}
