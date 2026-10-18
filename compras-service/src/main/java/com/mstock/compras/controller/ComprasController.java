package com.mstock.compras.controller;

import com.mstock.compras.dto.CompraRequest;
import com.mstock.compras.dto.CompraResponse;
import com.mstock.compras.service.ComprasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class ComprasController {
    private final ComprasService service;

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request,
                                                    Authentication authentication) {
        String token = ((Jwt) authentication.getPrincipal()).getTokenValue();
        CompraResponse compra = service.registrar(request, authentication.getName(), token);
        return ResponseEntity.created(URI.create("/api/compras/" + compra.id())).body(compra);
    }

    @GetMapping
    public List<CompraResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public CompraResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
