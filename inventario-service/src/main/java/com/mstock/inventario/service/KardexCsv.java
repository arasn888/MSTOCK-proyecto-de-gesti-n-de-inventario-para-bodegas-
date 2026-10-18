package com.mstock.inventario.service;

import com.mstock.inventario.client.ProductoCatalogo;
import com.mstock.inventario.dto.MovimientoResponse;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Kardex de un producto en CSV, listo para abrir en Excel: UTF-8 con BOM (para que respete las tildes),
 * separador coma y fin de línea CRLF. Los movimientos se escriben del más antiguo al más reciente, como
 * se lee un kardex. Es una clase sin estado ni dependencias para poder probarla sola.
 */
public final class KardexCsv {

    /** Archivo listo para descargar. */
    public record Archivo(String nombre, byte[] contenido) {
    }

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SALTO = "\r\n";

    private KardexCsv() {
    }

    /** @param movimientosAscendentes del más antiguo al más reciente */
    public static Archivo generar(ProductoCatalogo producto, List<MovimientoResponse> movimientosAscendentes) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        fila(csv, "Kardex");
        fila(csv, "Producto", producto.codigo() + " - " + producto.nombre());
        fila(csv, "Movimientos", String.valueOf(movimientosAscendentes.size()));
        csv.append(SALTO);
        fila(csv, "Fecha", "Tipo", "Cantidad", "Stock anterior", "Stock resultante", "Motivo", "Usuario");
        for (MovimientoResponse m : movimientosAscendentes) {
            fila(csv, m.fecha() == null ? "" : FECHA.format(m.fecha()), m.tipo().name(),
                    String.valueOf(m.cantidad()), String.valueOf(m.stockAnterior()),
                    String.valueOf(m.stockResultante()), m.motivo(), m.usuario());
        }
        return new Archivo(nombreArchivo(producto.codigo()), csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** El código viene del catálogo y va dentro de una cabecera HTTP: solo letras, números, guion y guion bajo. */
    static String nombreArchivo(String codigo) {
        String limpio = codigo == null ? "" : codigo.replaceAll("[^A-Za-z0-9_-]", "_");
        return "kardex-" + (limpio.isBlank() ? "producto" : limpio) + ".csv";
    }

    private static void fila(StringBuilder csv, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(campo(campos[i]));
        }
        csv.append(SALTO);
    }

    /**
     * Entrecomilla si hace falta y neutraliza las "fórmulas": un motivo que empiece con = + - @ se ejecutaría
     * como fórmula al abrirlo en Excel, y el motivo lo escribe un usuario.
     */
    static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
