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
import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.exception.ResultadoIncierto;
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
import java.util.UUID;

/**
 * Orquesta la venta. Este método NO es @Transactional: habla con otros servicios y no debe
 * mantener una transacción abierta mientras espera.
 *
 * Pasos: 1) cliente  2) precio vigente de cada producto (catálogo)
 *        3) descontar el stock, todo o nada (inventario)  4) guardar la venta (BD local)
 * El descuento lleva un id de operación (UUID) que el inventario trata como idempotente: repetirlo no
 * descuenta dos veces. Si el paso 4 falla después de descontar, o si del paso 3 no se supo el resultado
 * (timeout), se deshace por ese id (compensación): devuelve solo lo que realmente se descontó.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

    private final ClienteService clienteService;
    private final CatalogoGateway catalogo;
    private final InventarioGateway inventario;
    private final VentaRegistro registro;
    private final VentaAnulacion anulacion;
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

        String operacionId = UUID.randomUUID().toString();
        try {
            inventario.descontar(operacionId, items, token);
        } catch (ResultadoIncierto e) {
            // No se sabe si el inventario descontó: se deshace por id (no hace nada si nunca se aplicó).
            compensar(operacionId, "Reversión de venta sin confirmar (timeout)", e);
            throw e;
        }

        try {
            return registro.guardar(request.clienteId(), usuario, operacionId, lineas);
        } catch (RuntimeException e) {
            compensar(operacionId, "Reversión de venta no registrada", e);
            throw e;
        }
    }

    /**
     * Anula una venta y devuelve su stock. Solo ADMIN (ver SecurityConfig). Es idempotente: anular una venta
     * ya anulada devuelve su estado actual sin tocar el inventario, y si se repite por un fallo a medias, el
     * inventario reconoce la operación y no devuelve el stock dos veces.
     *
     * Orden: primero se devuelve el stock y después se marca la venta. Si algo falla en medio queda
     * COMPLETADA y se puede reintentar sin riesgo. Lo contrario (marcar primero) dejaría una venta
     * ANULADA con el stock sin devolver y sin forma de reintentar.
     */
    public VentaResponse anular(Long id, String motivo, String usuario) {
        VentaAnulacion.Pendiente venta = anulacion.preparar(id);
        if (venta.estado() == EstadoVenta.ANULADA) {
            return anulacion.consultar(id);
        }
        String motivoStock = "Anulación de venta " + venta.numero();
        String tokenInterno = tokenServicio.generar();
        if (venta.operacionId() != null) {
            inventario.revertirOperacion(venta.operacionId(), motivoStock, tokenInterno);
        } else {
            // Venta anterior al control de operaciones: se devuelve por lista, con una clave fija por venta
            // para que repetir la anulación no devuelva el stock otra vez.
            inventario.revertir(venta.items(), motivoStock, String.format("ANUL-%08d", venta.id()), tokenInterno);
        }
        return anulacion.marcarAnulada(id, motivo.trim(), usuario);
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

    private void compensar(String operacionId, String motivo, RuntimeException causa) {
        try {
            // La reversión usa la cuenta de servicio: el cajero no tiene permiso de devolver stock por su cuenta.
            inventario.revertirOperacion(operacionId, motivo, tokenServicio.generar());
            log.warn("La venta no se completó ({}); la operación {} fue revertida en el inventario",
                    causa.getMessage(), operacionId);
        } catch (RuntimeException ex) {
            log.error("REVERSIÓN PENDIENTE: la venta no se completó y el inventario no confirmó la reversión. "
                    + "Operación: {}. Causa original: {}. Fallo de la reversión: {}. "
                    + "Se puede repetir con POST /api/inventario/operaciones/{}/reversion (ADMIN)",
                    operacionId, causa.getMessage(), ex.getMessage(), operacionId);
        }
    }
}
