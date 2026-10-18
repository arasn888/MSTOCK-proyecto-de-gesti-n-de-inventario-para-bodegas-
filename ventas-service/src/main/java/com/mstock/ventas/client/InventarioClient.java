package com.mstock.ventas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "inventario-service", url = "${mstock.inventario.url:}", path = "/api/inventario")
public interface InventarioClient {

    /** Descuenta varios productos en una sola transacción: todos o ninguno. */
    @PostMapping("/salidas/lote")
    void salidaLote(@RequestBody LoteRequest request,
                    @RequestHeader("Authorization") String authorization);

    /** Devuelve stock por lista de productos (ventas anteriores al control de operaciones). */
    @PostMapping("/reversiones")
    void reversion(@RequestBody LoteRequest request,
                   @RequestHeader("Authorization") String authorization);

    /** Deshace lo que una operación descontó realmente. Idempotente; seguro si no se sabe si llegó a aplicarse. */
    @PostMapping("/operaciones/{operacionId}/reversion")
    void reversionDeOperacion(@PathVariable("operacionId") String operacionId,
                              @RequestBody ReversionOperacionRequest request,
                              @RequestHeader("Authorization") String authorization);
}
