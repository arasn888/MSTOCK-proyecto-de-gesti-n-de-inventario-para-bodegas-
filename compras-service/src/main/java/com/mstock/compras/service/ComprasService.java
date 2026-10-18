package com.mstock.compras.service;

import com.mstock.compras.client.CatalogoClient;
import com.mstock.compras.client.ProductoCatalogo;
import com.mstock.compras.client.ProveedorCatalogo;
import com.mstock.compras.dto.CompraRequest;
import com.mstock.compras.dto.CompraResponse;
import com.mstock.compras.entity.Compra;
import com.mstock.compras.entity.DetalleCompra;
import com.mstock.compras.entity.EstadoCompra;
import com.mstock.compras.event.CompraRegistradaEvent;
import com.mstock.compras.messaging.CompraEventPublisher;
import com.mstock.compras.repository.CompraRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprasService {
    private final CompraRepository repository;
    private final CatalogoClient catalogo;
    private final CompraEventPublisher publisher;

    /**
     * Sin @Transactional a propósito: las llamadas a catálogo (red) no deben mantener una conexión
     * a la base abierta. Solo el guardado final es una transacción (la del repositorio), y el
     * evento sale después de ese commit.
     */
    public CompraResponse registrar(CompraRequest request, String usuario, String token) {
        String authorization = "Bearer " + token;
        ProveedorCatalogo proveedor;
        try {
            proveedor = catalogo.obtenerProveedor(request.proveedorId(), authorization);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El proveedor no existe");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo validar el proveedor en catalogo-service");
        }
        if (!proveedor.activo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El proveedor está inactivo");
        }

        Compra compra = new Compra();
        compra.setProveedorId(proveedor.id());
        compra.setUsuario(usuario);
        BigDecimal total = BigDecimal.ZERO;

        for (var item : request.items()) {
            ProductoCatalogo producto;
            try {
                producto = catalogo.obtenerProducto(item.productoId(), authorization);
            } catch (FeignException.NotFound e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el producto " + item.productoId());
            } catch (FeignException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "No se pudo validar el producto " + item.productoId() + " en catálogo");
            }
            if (!producto.activo()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El producto " + producto.id() + " está inactivo");
            }
            if (producto.precioCompra() == null || producto.precioCompra().signum() < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El producto " + producto.id() + " no tiene un precio de compra válido");
            }
            if (producto.proveedorId() != null && !producto.proveedorId().equals(proveedor.id())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El producto " + producto.nombre() + " está asociado a otro proveedor");
            }

            DetalleCompra detalle = new DetalleCompra();
            detalle.setProductoId(producto.id());
            detalle.setProductoNombre(producto.nombre());
            detalle.setCantidad(item.cantidad());
            detalle.setCostoUnitario(producto.precioCompra());
            detalle.setSubtotal(producto.precioCompra().multiply(BigDecimal.valueOf(item.cantidad())));
            compra.agregarDetalle(detalle);
            total = total.add(detalle.getSubtotal());
        }

        compra.setTotal(total);
        Compra guardada = repository.saveAndFlush(compra);
        List<CompraRegistradaEvent.ItemCompraEvent> items = guardada.getDetalles().stream()
                .map(d -> new CompraRegistradaEvent.ItemCompraEvent(d.getProductoId(), d.getProductoNombre(),
                        d.getCantidad(), d.getCostoUnitario()))
                .toList();
        publisher.publicar(new CompraRegistradaEvent(guardada.getId(), guardada.getProveedorId(),
                guardada.getUsuario(), items));
        return CompraResponse.desde(guardada);
    }

    /** Aplica el resultado que informó el inventario. Idempotente: solo una compra PENDIENTE cambia. */
    @Transactional
    public void aplicarResultado(Long compraId, boolean aplicada, String motivo) {
        Compra compra = repository.findById(compraId).orElse(null);
        if (compra == null) {
            log.warn("Resultado para una compra que no existe: {}", compraId);
            return;
        }
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            log.info("Compra {} ya estaba {}: se ignora el resultado repetido", compraId, compra.getEstado());
            return;
        }
        if (aplicada) {
            compra.setEstado(EstadoCompra.RECIBIDA);
        } else {
            compra.setEstado(EstadoCompra.RECHAZADA);
            String m = (motivo == null || motivo.isBlank()) ? "El inventario no pudo aplicar la compra" : motivo.trim();
            compra.setMotivoRechazo(m.length() > 255 ? m.substring(0, 255) : m);
        }
        repository.save(compra);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar() {
        return repository.findAll().stream().map(CompraResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        return repository.findById(id).map(CompraResponse::desde)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la compra " + id));
    }
}
