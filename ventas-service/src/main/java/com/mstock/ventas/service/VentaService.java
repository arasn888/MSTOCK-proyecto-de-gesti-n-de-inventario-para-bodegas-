package com.mstock.ventas.service;

import com.mstock.ventas.client.CatalogoGateway;
import com.mstock.ventas.client.InventarioGateway;
import com.mstock.ventas.client.ItemLote;
import com.mstock.ventas.client.ProductoCatalogo;
import com.mstock.ventas.config.TokenServicio;
import com.mstock.ventas.dto.ItemVentaRequest;
import com.mstock.ventas.dto.VentaRequest;
import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.dto.VentaResumen;
import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Orquesta la venta. Este método NO es @Transactional: habla con otros servicios y no debe
 * mantener una transacción abierta mientras espera.
 *
 * Pasos: 1) cliente  2) precio vigente de cada producto (catálogo)
 *        3) descontar el stock, todo o nada (inventario)  4) guardar la venta (BD local)
 * Si el paso 4 falla después de descontar, se devuelve el stock (compensación).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

    private final ClienteService clienteService;
    private final CatalogoGateway catalogo;
    private final InventarioGateway inventario;
    private final VentaRegistro registro;
    private final VentaRepository ventaRepository;
    private final TokenServicio tokenServicio;

    public VentaResponse registrar(VentaRequest request, String usuario, String token) {
        if (request.clienteId() != null) {
            clienteService.buscarActivo(request.clienteId());
        }

        List<LineaVenta> lineas = new ArrayList<>();
        for (Map.Entry<Long, Integer> item : consolidar(request.items()).entrySet()) {
            ProductoCatalogo producto = catalogo.consultarProducto(item.getKey(), token);
            if (!producto.activo()) {
                throw new ReglaNegocioException("El producto " + producto.codigo()
                        + " está inactivo y no se puede vender");
            }
            lineas.add(new LineaVenta(producto, item.getValue()));
        }
        List<ItemLote> items = lineas.stream()
                .map(l -> new ItemLote(l.producto().id(), l.cantidad()))
                .toList();

        inventario.descontar(items, token);

        try {
            return registro.guardar(request.clienteId(), usuario, lineas);
        } catch (RuntimeException e) {
            compensar(items, token, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return ventaRepository.buscarConDetalle(id)
                .map(VentaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id));
    }

    /** Ventas entre dos fechas (ambas incluidas). Sin fechas: las de hoy. */
    @Transactional(readOnly = true)
    public List<VentaResumen> listar(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin;
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        return ventaRepository.buscarPorRango(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay())
                .stream().map(VentaResumen::desde).toList();
    }

    /** Une los productos repetidos sumando cantidades; ordenado por id. */
    private Map<Long, Integer> consolidar(List<ItemVentaRequest> items) {
        Map<Long, Long> sumas = new TreeMap<>();
        for (ItemVentaRequest item : items) {
            sumas.merge(item.productoId(), (long) item.cantidad(), Long::sum);
        }
        Map<Long, Integer> resultado = new TreeMap<>();
        sumas.forEach((productoId, cantidad) -> {
            if (cantidad > 10000L) {
                throw new ReglaNegocioException(
                        "La cantidad total del producto " + productoId + " no puede superar 10 000");
            }
            resultado.put(productoId, cantidad.intValue());
        });
        return resultado;
    }

    private void compensar(List<ItemLote> items, String token, RuntimeException causa) {
        try {
            // La reversión usa la cuenta de servicio: el cajero no tiene permiso de devolver stock por su cuenta.
            inventario.revertir(items, tokenServicio.generar());
            log.warn("La venta no se pudo guardar ({}); el stock fue devuelto", causa.getMessage());
        } catch (RuntimeException ex) {
            log.error("REVERSIÓN PENDIENTE: la venta no se guardó y el stock no pudo devolverse. "
                    + "Productos: {}. Causa original: {}. Fallo de la reversión: {}",
                    items, causa.getMessage(), ex.getMessage());
        }
    }
}
