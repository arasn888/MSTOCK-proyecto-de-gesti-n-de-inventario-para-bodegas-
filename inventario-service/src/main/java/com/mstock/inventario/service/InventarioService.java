package com.mstock.inventario.service;

import com.mstock.inventario.dto.AjusteRequest;
import com.mstock.inventario.dto.ItemLote;
import com.mstock.inventario.dto.LoteRequest;
import com.mstock.inventario.dto.MovimientoRequest;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.dto.StockMinimoRequest;
import com.mstock.inventario.dto.StockResponse;
import com.mstock.inventario.entity.Movimiento;
import com.mstock.inventario.entity.Stock;
import com.mstock.inventario.entity.TipoMovimiento;
import com.mstock.inventario.exception.RecursoNoEncontradoException;
import com.mstock.inventario.exception.ReglaNegocioException;
import com.mstock.inventario.exception.StockInsuficienteException;
import com.mstock.inventario.repository.MovimientoRepository;
import com.mstock.inventario.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class InventarioService {

    static final int STOCK_MAXIMO = 10_000_000;

    private final StockRepository stockRepository;
    private final MovimientoRepository movimientoRepository;

    @Transactional(readOnly = true)
    public List<StockResponse> listar() {
        return stockRepository.findAllByOrderByProductoIdAsc().stream()
                .map(StockResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockResponse obtener(Long productoId) {
        return StockResponse.desde(buscar(productoId));
    }

    @Transactional(readOnly = true)
    public List<StockResponse> alertas() {
        return stockRepository.findEnAlerta().stream()
                .map(StockResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> kardex(Long productoId) {
        buscar(productoId);
        return movimientoRepository.findByProductoIdOrderByFechaDescIdDesc(productoId).stream()
                .map(MovimientoResponse::desde)
                .toList();
    }

    @Transactional
    public MovimientoResponse registrarEntrada(MovimientoRequest request, String usuario) {
        Stock stock = stockRepository.buscarParaActualizar(request.productoId())
                .orElseGet(() -> crearStock(request.productoId()));

        long nuevo = (long) stock.getCantidad() + request.cantidad();
        if (nuevo > STOCK_MAXIMO) {
            throw new ReglaNegocioException("El stock no puede superar " + STOCK_MAXIMO + " unidades");
        }
        return aplicar(stock, TipoMovimiento.ENTRADA, request.cantidad(), (int) nuevo,
                request.motivo(), usuario);
    }

    @Transactional
    public MovimientoResponse registrarSalida(MovimientoRequest request, String usuario) {
        Stock stock = stockRepository.buscarParaActualizar(request.productoId())
                .orElseThrow(() -> new StockInsuficienteException(0, request.cantidad()));

        if (request.cantidad() > stock.getCantidad()) {
            throw new StockInsuficienteException(stock.getCantidad(), request.cantidad());
        }
        return aplicar(stock, TipoMovimiento.SALIDA, request.cantidad(),
                stock.getCantidad() - request.cantidad(), request.motivo(), usuario);
    }

    /**
     * Salida de varios productos en una sola transacción: si uno no alcanza, no se descuenta ninguno.
     * Los productos se procesan en orden de id para que dos ventas simultáneas con los mismos
     * productos bloqueen las filas en el mismo orden y no se queden esperándose entre sí (deadlock).
     */
    @Transactional
    public List<MovimientoResponse> registrarSalidaLote(LoteRequest request, String usuario) {
        List<MovimientoResponse> resultado = new ArrayList<>();
        for (ItemLote item : consolidar(request.items())) {
            resultado.add(registrarSalida(
                    new MovimientoRequest(item.productoId(), item.cantidad(), request.motivo()), usuario));
        }
        return resultado;
    }

    /** Devuelve stock de varios productos de una vez (reversión de una venta). Todo o nada. */
    @Transactional
    public List<MovimientoResponse> registrarEntradaLote(LoteRequest request, String usuario) {
        List<MovimientoResponse> resultado = new ArrayList<>();
        for (ItemLote item : consolidar(request.items())) {
            resultado.add(registrarEntrada(
                    new MovimientoRequest(item.productoId(), item.cantidad(), request.motivo()), usuario));
        }
        return resultado;
    }

    /** Une los productos repetidos sumando sus cantidades y los ordena por id. */
    private List<ItemLote> consolidar(List<ItemLote> items) {
        Map<Long, Long> sumas = new TreeMap<>();
        for (ItemLote item : items) {
            sumas.merge(item.productoId(), (long) item.cantidad(), Long::sum);
        }
        List<ItemLote> resultado = new ArrayList<>();
        sumas.forEach((productoId, cantidad) -> {
            if (cantidad > 1_000_000L) {
                throw new ReglaNegocioException(
                        "La cantidad total del producto " + productoId + " no puede superar 1 000 000");
            }
            resultado.add(new ItemLote(productoId, cantidad.intValue()));
        });
        return resultado;
    }

    /** Corrige el stock con el conteo físico real. Registra la diferencia en el kardex. */
    @Transactional
    public MovimientoResponse registrarAjuste(AjusteRequest request, String usuario) {
        Stock stock = stockRepository.buscarParaActualizar(request.productoId())
                .orElseGet(() -> crearStock(request.productoId()));

        if (request.cantidadReal() == stock.getCantidad()) {
            throw new ReglaNegocioException(
                    "El stock real coincide con el stock actual: no hay nada que ajustar");
        }
        int diferencia = Math.abs(request.cantidadReal() - stock.getCantidad());
        return aplicar(stock, TipoMovimiento.AJUSTE, diferencia, request.cantidadReal(),
                request.motivo(), usuario);
    }

    @Transactional
    public StockResponse actualizarStockMinimo(Long productoId, StockMinimoRequest request) {
        Stock stock = buscar(productoId);
        stock.setStockMinimo(request.stockMinimo());
        stockRepository.saveAndFlush(stock);
        return StockResponse.desde(stock);
    }

    private Stock buscar(Long productoId) {
        return stockRepository.findByProductoId(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El producto " + productoId + " no tiene registro de inventario"));
    }

    private Stock crearStock(Long productoId) {
        Stock nuevo = Stock.builder().productoId(productoId).build();
        stockRepository.saveAndFlush(nuevo);
        return nuevo;
    }

    private MovimientoResponse aplicar(Stock stock, TipoMovimiento tipo, int cantidad,
                                       int nuevoStock, String motivo, String usuario) {
        int anterior = stock.getCantidad();
        stock.setCantidad(nuevoStock);
        stockRepository.saveAndFlush(stock);

        Movimiento movimiento = movimientoRepository.save(Movimiento.builder()
                .productoId(stock.getProductoId())
                .tipo(tipo)
                .cantidad(cantidad)
                .stockAnterior(anterior)
                .stockResultante(nuevoStock)
                .motivo(motivo == null || motivo.isBlank() ? null : motivo.trim())
                .usuario(usuario)
                .build());
        return MovimientoResponse.desde(movimiento);
    }
}
