package com.mstock.notificaciones.controller;

import com.mstock.notificaciones.dto.LecturaMasivaResponse;
import com.mstock.notificaciones.dto.NotificacionResponse;
import com.mstock.notificaciones.dto.ResumenResponse;
import com.mstock.notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public List<NotificacionResponse> listar(
            @RequestParam(defaultValue = "false") boolean soloNoLeidas) {
        return notificacionService.listar(soloNoLeidas);
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return notificacionService.resumen();
    }

    @PutMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(@PathVariable Long id) {
        return notificacionService.marcarLeida(id);
    }

    @PutMapping("/leer-todas")
    public LecturaMasivaResponse marcarTodasLeidas() {
        return notificacionService.marcarTodasLeidas();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        notificacionService.eliminar(id);
    }
}
