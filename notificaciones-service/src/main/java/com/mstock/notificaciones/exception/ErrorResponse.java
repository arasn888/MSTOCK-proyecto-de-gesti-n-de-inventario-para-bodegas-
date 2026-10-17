package com.mstock.notificaciones.exception;

import java.time.Instant;

public record ErrorResponse(int estado, String mensaje, Instant fecha) {
}
