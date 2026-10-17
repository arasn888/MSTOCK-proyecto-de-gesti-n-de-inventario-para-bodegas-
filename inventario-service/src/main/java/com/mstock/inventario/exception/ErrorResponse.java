package com.mstock.inventario.exception;

import java.time.Instant;

public record ErrorResponse(int estado, String mensaje, Instant fecha) {
}
