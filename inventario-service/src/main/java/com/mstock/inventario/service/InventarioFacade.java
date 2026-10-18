package com.mstock.inventario.service;

import com.mstock.inventario.client.CatalogoGateway;
import com.mstock.inventario.client.ProductoCatalogo;
import com.mstock.inventario.dto.AjusteRequest;
import com.mstock.inventario.dto.ItemLote;
import com.mstock.inventario.dto.LoteRequest;
import com.mstock.inventario.dto.MovimientoRequest;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Valida el producto en el catálogo y recién después mueve el stock.
 * No es @Transactional a propósito: la llamada remota ocurre antes de abrir la
 * transacción del InventarioService, así la fila de stock no queda bloqueada
 * mientras se espera la respuesta de otro servicio.
 */
@Service
@RequiredArgsConstructor
public class InventarioFacade {

    private final CatalogoGateway catalogo;
    private final InventarioService inventario;

    public MovimientoResponse registrarEntrada(MovimientoRequest request, String usuario, String token) {
        validarProducto(request.productoId(), token);
        return inventario.registrarEntrada(request, usuario);
    }

    public MovimientoResponse registrarSalida(MovimientoRequest request, String usuario, String token) {
        validarProducto(request.productoId(), token);
        return inventario.registrarSalida(request, usuario);
    }

    public MovimientoResponse registrarAjuste(AjusteRequest request, String usuario, String token) {
        validarProducto(request.productoId(), token);
        return inventario.registrarAjuste(request, usuario);
    }

    /** Salida de varios productos (venta): cada producto distinto se valida en el catálogo. */
    public List<MovimientoResponse> registrarSalidaLote(LoteRequest request, String usuario, String token) {
        request.items().stream().map(ItemLote::productoId).distinct()
                .forEach(productoId -> validarProducto(productoId, token));
        return inventario.registrarSalidaLote(request, usuario);
    }

    /**
     * Reversión de una venta fallida. No consulta el catálogo: aunque el producto se haya
     * desactivado mientras tanto, el stock descontado siempre debe poder devolverse.
     */
    public List<MovimientoResponse> registrarReversion(LoteRequest request, String usuario) {
        return inventario.registrarEntradaLote(request, usuario);
    }

    /** Deshace por id de operación lo que se descontó realmente. No consulta el catálogo (ver registrarReversion). */
    public List<MovimientoResponse> revertirOperacion(String operacionId, String motivo, String usuario) {
        return inventario.revertirOperacion(operacionId, motivo, usuario);
    }

    private void validarProducto(Long productoId, String token) {
        ProductoCatalogo producto = catalogo.consultarProducto(productoId, token);
        if (!producto.activo()) {
            throw new ReglaNegocioException("El producto " + producto.codigo()
                    + " está inactivo en el catálogo y no admite movimientos de inventario");
        }
    }
}
