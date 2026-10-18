package com.mstock.inventario.service;

import com.mstock.inventario.client.ProductoCatalogo;
import com.mstock.inventario.dto.MovimientoResponse;
import com.mstock.inventario.entity.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KardexCsvTest {

    private static final ProductoCatalogo ARROZ = new ProductoCatalogo(1L, "ARR-1", "Arroz extra", true);

    private MovimientoResponse mov(long id, TipoMovimiento tipo, int cantidad, int anterior, int resultante,
                                   String motivo, LocalDateTime fecha) {
        return new MovimientoResponse(id, 1L, tipo, cantidad, anterior, resultante, motivo, "admin", fecha);
    }

    private String texto(KardexCsv.Archivo archivo) {
        return new String(archivo.contenido(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("El archivo lleva BOM UTF-8, el producto, la cabecera y los movimientos en el orden recibido")
    void estructura() {
        KardexCsv.Archivo archivo = KardexCsv.generar(ARROZ, List.of(
                mov(1, TipoMovimiento.ENTRADA, 10, 0, 10, "Stock inicial", LocalDateTime.of(2026, 10, 1, 9, 30, 5)),
                mov(2, TipoMovimiento.SALIDA, 3, 10, 7, "Venta de mostrador", LocalDateTime.of(2026, 10, 2, 11, 0))));

        String csv = texto(archivo);
        assertThat(csv).startsWith("\uFEFFKardex\r\n");
        assertThat(csv).contains("Producto,ARR-1 - Arroz extra\r\n");
        assertThat(csv).contains("Movimientos,2\r\n");
        assertThat(csv).contains("Fecha,Tipo,Cantidad,Stock anterior,Stock resultante,Motivo,Usuario\r\n");
        assertThat(csv.indexOf("Stock inicial")).isLessThan(csv.indexOf("Venta de mostrador"));
        assertThat(csv).contains("2026-10-01 09:30:05,ENTRADA,10,0,10,Stock inicial,admin\r\n");
        assertThat(csv).contains("2026-10-02 11:00:00,SALIDA,3,10,7,Venta de mostrador,admin\r\n");
        assertThat(archivo.contenido()[0]).isEqualTo((byte) 0xEF); // BOM
    }

    @Test
    @DisplayName("Las tildes sobreviven (UTF-8) y un kardex sin movimientos solo trae la cabecera")
    void tildesYVacio() {
        KardexCsv.Archivo archivo = KardexCsv.generar(
                new ProductoCatalogo(2L, "AZ-1", "Azúcar rubia", true), List.of());

        assertThat(texto(archivo)).contains("Azúcar rubia").contains("Movimientos,0");
        assertThat(texto(archivo).lines().filter(l -> l.matches("^\\d{4}-.*")).count()).isZero();
    }

    @Test
    @DisplayName("Campos con coma, comillas o saltos de línea se entrecomillan")
    void escapado() {
        assertThat(KardexCsv.campo("Compra, lote 2")).isEqualTo("\"Compra, lote 2\"");
        assertThat(KardexCsv.campo("Dijo \"ok\"")).isEqualTo("\"Dijo \"\"ok\"\"\"");
        assertThat(KardexCsv.campo("línea1\nlínea2")).isEqualTo("\"línea1\nlínea2\"");
        assertThat(KardexCsv.campo("simple")).isEqualTo("simple");
        assertThat(KardexCsv.campo(null)).isEmpty();
    }

    @Test
    @DisplayName("Un motivo que empieza como fórmula de Excel se neutraliza")
    void formulas() {
        assertThat(KardexCsv.campo("=HYPERLINK(\"http://x\")")).startsWith("\"'=HYPERLINK");
        assertThat(KardexCsv.campo("+51999")).isEqualTo("'+51999");
        assertThat(KardexCsv.campo("-5 cajas")).isEqualTo("'-5 cajas");
        assertThat(KardexCsv.campo("@usuario")).isEqualTo("'@usuario");
        assertThat(KardexCsv.campo("Venta =normal")).isEqualTo("Venta =normal");
    }

    @Test
    @DisplayName("El nombre del archivo solo usa caracteres seguros para una cabecera HTTP")
    void nombreDeArchivo() {
        assertThat(KardexCsv.nombreArchivo("ARR-1")).isEqualTo("kardex-ARR-1.csv");
        assertThat(KardexCsv.nombreArchivo("A/B \"x\"\r\nC")).isEqualTo("kardex-A_B__x___C.csv");
        assertThat(KardexCsv.nombreArchivo(null)).isEqualTo("kardex-producto.csv");
        assertThat(KardexCsv.nombreArchivo("  ")).isEqualTo("kardex-__.csv");
    }
}
