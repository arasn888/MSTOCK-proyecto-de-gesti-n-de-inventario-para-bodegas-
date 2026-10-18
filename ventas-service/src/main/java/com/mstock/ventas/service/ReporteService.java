package com.mstock.ventas.service;

import com.mstock.ventas.dto.CierreCajaResponse;
import com.mstock.ventas.dto.DashboardResponse;
import com.mstock.ventas.entity.EstadoVenta;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.repository.DetalleVentaRepository;
import com.mstock.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    static final int MAXIMO_DIAS = 366;
    static final int TOP_POR_DEFECTO = 5;
    static final int TOP_MAXIMO = 50;

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleRepository;

    /** Sin fechas: hoy. Solo desde: de esa fecha a hoy. El rango incluye ambos días. */
    public DashboardResponse dashboard(LocalDate desde, LocalDate hasta, Integer top) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin;
        validarRango(inicio, fin);
        int limite = top == null ? TOP_POR_DEFECTO : top;
        if (limite < 1 || limite > TOP_MAXIMO) {
            throw new ReglaNegocioException("El top debe estar entre 1 y " + TOP_MAXIMO);
        }
        LocalDateTime d = inicio.atStartOfDay();
        LocalDateTime h = fin.plusDays(1).atStartOfDay();

        VentaRepository.ResumenVentas completadas = ventaRepository.resumir(EstadoVenta.COMPLETADA, d, h);
        VentaRepository.ResumenVentas anuladas = ventaRepository.resumir(EstadoVenta.ANULADA, d, h);
        DetalleVentaRepository.Rentabilidad renta = detalleRepository.rentabilidad(d, h);

        long cantidad = cantidad(completadas.getCantidad());
        BigDecimal total = dinero(completadas.getTotal());
        BigDecimal ticket = cantidad == 0 ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);

        List<DashboardResponse.ProductoMasVendido> masVendidos = detalleRepository
                .masVendidos(d, h, PageRequest.of(0, limite)).stream()
                .map(p -> new DashboardResponse.ProductoMasVendido(p.getProductoId(), p.getCodigo(), p.getNombre(),
                        cantidad(p.getCantidad()), dinero(p.getIngresos())))
                .toList();

        return new DashboardResponse(inicio, fin,
                new DashboardResponse.Ventas(cantidad, total, ticket,
                        dinero(renta.getGanancia()), cantidad(renta.getLineasSinCosto())),
                new DashboardResponse.Anuladas(cantidad(anuladas.getCantidad()), dinero(anuladas.getTotal())),
                masVendidos);
    }

    /**
     * Cierre de caja del día (sin fecha: hoy). Si {@code soloUsuario} no es null, solo incluye a ese cajero:
     * así un cajero ve su propio cierre y no el de sus compañeros.
     */
    public CierreCajaResponse cierreCaja(LocalDate fecha, String soloUsuario) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        LocalDateTime d = dia.atStartOfDay();
        LocalDateTime h = dia.plusDays(1).atStartOfDay();

        Map<String, long[]> cantidades = new TreeMap<>();      // [vendidas, anuladas]
        Map<String, BigDecimal[]> montos = new TreeMap<>();    // [vendido, anulado]
        for (VentaRepository.TotalCajero fila : ventaRepository.totalesPorCajero(d, h)) {
            if (soloUsuario != null && !soloUsuario.equals(fila.getUsuario())) {
                continue;
            }
            long[] c = cantidades.computeIfAbsent(fila.getUsuario(), u -> new long[2]);
            BigDecimal[] m = montos.computeIfAbsent(fila.getUsuario(),
                    u -> new BigDecimal[]{dinero(null), dinero(null)});
            int i = fila.getEstado() == EstadoVenta.COMPLETADA ? 0 : 1;
            c[i] += cantidad(fila.getCantidad());
            m[i] = m[i].add(dinero(fila.getTotal()));
        }

        List<CierreCajaResponse.Cajero> cajeros = new ArrayList<>();
        long ventas = 0;
        long anuladas = 0;
        BigDecimal vendido = dinero(null);
        BigDecimal anulado = dinero(null);
        for (Map.Entry<String, long[]> e : cantidades.entrySet()) {
            long[] c = e.getValue();
            BigDecimal[] m = montos.get(e.getKey());
            cajeros.add(new CierreCajaResponse.Cajero(e.getKey(), c[0], m[0], c[1], m[1]));
            ventas += c[0];
            anuladas += c[1];
            vendido = vendido.add(m[0]);
            anulado = anulado.add(m[1]);
        }
        return new CierreCajaResponse(dia, cajeros,
                new CierreCajaResponse.Totales(ventas, vendido, anuladas, anulado));
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        if (inicio.plusDays(MAXIMO_DIAS).isBefore(fin)) {
            throw new ReglaNegocioException("El rango no puede superar " + MAXIMO_DIAS + " días");
        }
    }

    private static long cantidad(Long valor) {
        return valor == null ? 0L : valor;
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
