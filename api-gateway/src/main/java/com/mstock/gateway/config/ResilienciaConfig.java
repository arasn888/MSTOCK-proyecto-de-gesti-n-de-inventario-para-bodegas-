package com.mstock.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuración común de los circuit breakers de las rutas (uno por servicio: auth, catalogo,
 * inventario, ventas, notificaciones). Solo cuentan como fallo las caídas del servicio (sin instancia en Eureka,
 * conexión rechazada o tiempo agotado). Un 4xx o un 503 que el propio servicio devuelve con su
 * mensaje (por ejemplo ventas avisando que el inventario está caído) pasa tal cual.
 *
 * El límite de tiempo por defecto de Resilience4J es 1 s: muy poco para registrar una venta, que
 * llama a otros dos servicios. Por eso se amplía a 30 s (menos que el response-timeout de 35 s del cliente HTTP, para que el corte lo
 * decida siempre el circuit breaker).
 */
@Configuration
public class ResilienciaConfig {

    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerPorDefecto() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(15))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(30))
                        .build())
                .build());
    }
}
