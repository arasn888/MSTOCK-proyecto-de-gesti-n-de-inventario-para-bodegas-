package com.mstock.gateway.fallback;

import java.time.Instant;

public record ErrorResponse(int estado, String mensaje, Instant fecha) {
}
