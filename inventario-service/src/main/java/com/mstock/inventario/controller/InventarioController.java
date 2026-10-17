package com.mstock.inventario.controller;

import com.mstock.inventario.dto.AjusteRequest;
import com.mstock.inventario.dto.MovimientoRequest;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.dto.StockMinimoRequest;
import com.mstock.inventario.dto.StockResponse;
import com.mstock.inventario.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping
    public List<StockResponse> listar() {
        return inventarioService.listar();
    }

    @GetMapping("/alertas")
    public List<StockResponse> alertas() {
        return inventarioService.alertas();
    }

    @GetMapping("/{productoId}")
    public StockResponse obtener(@PathVariable Long productoId) {
        return inventarioService.obtener(productoId);
    }

    @GetMapping("/{productoId}/movimientos")
    public List<MovimientoResponse> kardex(@PathVariable Long productoId) {
        return inventarioService.kardex(productoId);
    }

    @PostMapping("/entradas")
    public ResponseEntity<MovimientoResponse> entrada(@Valid @RequestBody MovimientoRequest request,
                                                      Authentication authentication) {
        return creado(inventarioService.registrarEntrada(request, authentication.getName()));
    }

    @PostMapping("/salidas")
    public ResponseEntity<MovimientoResponse> salida(@Valid @RequestBody MovimientoRequest request,
                                                     Authentication authentication) {
        return creado(inventarioService.registrarSalida(request, authentication.getName()));
    }

    @PostMapping("/ajustes")
    public ResponseEntity<MovimientoResponse> ajuste(@Valid @RequestBody AjusteRequest request,
                                                     Authentication authentication) {
        return creado(inventarioService.registrarAjuste(request, authentication.getName()));
    }

    @PutMapping("/{productoId}/stock-minimo")
    public StockResponse stockMinimo(@PathVariable Long productoId,
                                     @Valid @RequestBody StockMinimoRequest request) {
        return inventarioService.actualizarStockMinimo(productoId, request);
    }

    private ResponseEntity<MovimientoResponse> creado(MovimientoResponse movimiento) {
        return ResponseEntity
                .created(URI.create("/api/inventario/" + movimiento.productoId() + "/movimientos"))
                .body(movimiento);
    }
}
