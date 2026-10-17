package com.mstock.ventas.config;

import com.mstock.ventas.client.FalloCatalogoException;
import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.exception.StockInsuficienteException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class ResilienciaConfig {

    /** Catálogo: ignora el 404 (producto inexistente), que no es una caída. */
    @Bean
    CircuitBreaker catalogoCircuitBreaker() {
        return crear("catalogo", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .ignoreExceptions(RecursoNoEncontradoException.class)
                .build());
    }

    /**
     * Inventario: ignora los errores de negocio (stock insuficiente, producto inactivo o inexistente):
     * el inventario respondió bien, solo dijo que no.
     */
    @Bean
    CircuitBreaker inventarioCircuitBreaker() {
        return crear("inventario", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .ignoreExceptions(StockInsuficienteException.class, ReglaNegocioException.class,
                        RecursoNoEncontradoException.class)
                .build());
    }

    /** Solo para el catálogo (lecturas): hasta 3 intentos con 300 ms entre ellos. */
    @Bean
    Retry catalogoRetry() {
        return Retry.of("catalogo", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(300))
                .retryExceptions(FalloCatalogoException.class)
                .build());
    }

    private CircuitBreaker crear(String nombre, CircuitBreakerConfig config) {
        CircuitBreaker cb = CircuitBreaker.of(nombre, config);
        cb.getEventPublisher().onStateTransition(evento ->
                log.warn("Circuit breaker '{}': {}", nombre, evento.getStateTransition()));
        return cb;
    }
}
