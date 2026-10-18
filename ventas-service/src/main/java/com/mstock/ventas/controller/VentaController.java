package com.mstock.ventas.controller;

import com.mstock.ventas.dto.AnulacionRequest;
import com.mstock.ventas.dto.VentaRequest;
import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.dto.VentaResumen;
import com.mstock.ventas.service.BoletaPdf;
import com.mstock.ventas.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final BoletaPdf boletaPdf;

    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request,
                                                   Authentication authentication) {
        String token = ((Jwt) authentication.getPrincipal()).getTokenValue();
        VentaResponse venta = ventaService.registrar(request, authentication.getName(), token);
        return ResponseEntity.created(URI.create("/api/ventas/" + venta.id())).body(venta);
    }

    /** Anula la venta y devuelve su stock (solo ADMIN). Repetirla es seguro. */
    @PostMapping("/{id}/anulacion")
    public VentaResponse anular(@PathVariable Long id, @Valid @RequestBody AnulacionRequest request,
                                Authentication authentication) {
        return ventaService.anular(id, request.motivo(), authentication.getName());
    }

    @GetMapping
    public List<VentaResumen> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ventaService.listar(desde, hasta);
    }

    /** Boleta de la venta en PDF (se abre en el navegador). Cualquier usuario autenticado, como el detalle de la venta. */
    @GetMapping("/{id}/boleta")
    public ResponseEntity<byte[]> boleta(@PathVariable Long id) {
        VentaResponse venta = ventaService.obtener(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("boleta-" + venta.numero() + ".pdf").build().toString())
                .body(boletaPdf.generar(venta));
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventaService.obtener(id);
    }
}
