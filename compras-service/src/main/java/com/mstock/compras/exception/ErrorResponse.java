package com.mstock.compras.exception;

import java.time.Instant;

public record ErrorResponse(int estado, String mensaje, Instant fecha) {
}
