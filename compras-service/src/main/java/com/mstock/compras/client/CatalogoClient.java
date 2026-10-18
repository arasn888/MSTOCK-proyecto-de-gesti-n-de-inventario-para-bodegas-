package com.mstock.compras.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalogo-service", url = "${mstock.catalogo.url:}")
public interface CatalogoClient {
    @GetMapping("/api/proveedores/{id}")
    ProveedorCatalogo obtenerProveedor(@PathVariable Long id,
                                      @RequestHeader("Authorization") String authorization);

    @GetMapping("/api/productos/{id}")
    ProductoCatalogo obtenerProducto(@PathVariable Long id,
                                    @RequestHeader("Authorization") String authorization);
}
