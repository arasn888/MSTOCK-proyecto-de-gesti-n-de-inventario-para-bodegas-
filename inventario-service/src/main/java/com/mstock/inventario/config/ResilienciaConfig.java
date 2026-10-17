package com.mstock.inventario.config;

import com.mstock.inventario.client.FalloCatalogoException;
import com.mstock.inventario.exception.RecursoNoEncontradoException;
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
        CircuitBreaker cb = CircuitBreaker.of("catalogo", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .ignoreExceptions(RecursoNoEncontradoException.class)
                .build());
        cb.getEventPublisher().onStateTransition(evento ->
                log.warn("Circuit breaker 'catalogo': {}", evento.getStateTransition()));
        return cb;
    }

    /** Solo lecturas: hasta 3 intentos con 300 ms entre ellos. */
    @Bean
    Retry catalogoRetry() {
        return Retry.of("catalogo", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(300))
                .retryExceptions(FalloCatalogoException.class)
                .build());
    }
}
