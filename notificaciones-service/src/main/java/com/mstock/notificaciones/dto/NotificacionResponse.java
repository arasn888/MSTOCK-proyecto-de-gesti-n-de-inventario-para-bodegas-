package com.mstock.notificaciones.dto;

import com.mstock.notificaciones.entity.Notificacion;
import com.mstock.notificaciones.entity.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        Long referenciaId,
        boolean leida,
        LocalDateTime fecha) {

    public static NotificacionResponse desde(Notificacion n) {
        return new NotificacionResponse(
                n.getId(), n.getTipo(), n.getTitulo(), n.getMensaje(),
                n.getReferenciaId(), n.isLeida(), n.getFecha());
    }
}
