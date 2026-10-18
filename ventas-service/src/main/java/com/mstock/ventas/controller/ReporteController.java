package com.mstock.ventas.controller;

import com.mstock.ventas.dto.CierreCajaResponse;
import com.mstock.ventas.dto.DashboardResponse;
import com.mstock.ventas.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /** Solo ADMIN (ver SecurityConfig). Sin fechas: hoy. */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer top) {
        return reporteService.dashboard(desde, hasta, top);
    }

    /** ADMIN ve el cierre de todos los cajeros; un CAJERO solo el suyo. */
    @GetMapping("/cierre-caja")
    public CierreCajaResponse cierreCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return reporteService.cierreCaja(fecha, admin ? null : authentication.getName());
    }
}
