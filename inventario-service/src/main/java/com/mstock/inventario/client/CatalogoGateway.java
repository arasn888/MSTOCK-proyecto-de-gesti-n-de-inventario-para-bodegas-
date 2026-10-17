package com.mstock.inventario.client;

import com.mstock.inventario.exception.RecursoNoEncontradoException;
import com.mstock.inventario.exception.ServicioNoDisponibleException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Validación de productos contra el catálogo. Es una lectura (GET), así que es seguro reintentarla:
 * Retry( CircuitBreaker( llamada ) ). Si el catálogo no responde, el movimiento se rechaza con 503:
 * es mejor no mover stock que mover el de un producto que no se pudo validar.
 */
@Slf4j
@Component
public class CatalogoGateway {

    private final ProductoCatalogoClient client;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public CatalogoGateway(ProductoCatalogoClient client,
                           @Qualifier("catalogoCircuitBreaker") CircuitBreaker circuitBreaker,
                           @Qualifier("catalogoRetry") Retry retry) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    public ProductoCatalogo consultarProducto(Long productoId, String token) {
        Supplier<ProductoCatalogo> llamada = () -> consultar(productoId, "Bearer " + token);
        Supplier<ProductoCatalogo> protegida =
                Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, llamada));
        try {
            return protegida.get();
        } catch (CallNotPermittedException e) {
            throw new ServicioNoDisponibleException(
                    "El catálogo no está respondiendo: no se puede validar el producto. "
                            + "No se registró ningún movimiento. Inténtalo en unos segundos");
        } catch (FalloCatalogoException e) {
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar el catálogo para validar el producto. No se registró ningún movimiento");
        }
    }

    private ProductoCatalogo consultar(Long productoId, String authorization) {
        try {
            return client.obtener(productoId, authorization);
        } catch (FeignException.NotFound e) {
            throw new RecursoNoEncontradoException("El producto " + productoId + " no existe en el catálogo");
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            throw new ServicioNoDisponibleException("El catálogo rechazó las credenciales enviadas por inventario");
        } catch (FeignException e) {
            log.warn("Fallo al consultar el catálogo (estado {}): {}", e.status(), e.getMessage());
            throw new FalloCatalogoException("Catálogo no disponible (estado " + e.status() + ")", e);
        }
    }
}
