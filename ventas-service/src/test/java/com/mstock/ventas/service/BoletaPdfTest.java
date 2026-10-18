package com.mstock.ventas.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.mstock.ventas.dto.DetalleResponse;
import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.entity.EstadoVenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoletaPdfTest {

    private final BoletaPdf boleta = new BoletaPdf("Bodega Don Pepe");

    private VentaResponse venta(EstadoVenta estado, String cliente, List<DetalleResponse> detalle) {
        boolean anulada = estado == EstadoVenta.ANULADA;
        return new VentaResponse(5L, "B001-00000005", cliente == null ? null : 1L, cliente, "cajero",
                LocalDateTime.of(2026, 10, 17, 14, 30), new BigDecimal("14.50"), estado,
                anulada ? "Error de digitación" : null,
                anulada ? LocalDateTime.of(2026, 10, 17, 15, 0) : null,
                anulada ? "admin" : null, detalle);
    }

    private List<DetalleResponse> detalle() {
        return List.of(
                new DetalleResponse(1L, "ARR-1", "Arroz extra 1 kg", 3, new BigDecimal("3.50"), new BigDecimal("10.50")),
                new DetalleResponse(2L, "AZ-1", "Azúcar rubia", 2, new BigDecimal("2.00"), new BigDecimal("4.00")));
    }

    private String texto(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder todo = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                todo.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return todo.toString();
        } finally {
            lector.close();
        }
    }

    @Test
    @DisplayName("Genera un PDF válido con negocio, número, cliente, productos y total")
    void boletaNormal() throws IOException {
        byte[] pdf = boleta.generar(venta(EstadoVenta.COMPLETADA, "María Pérez", detalle()));

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String texto = texto(pdf);
        assertThat(texto).contains("Bodega Don Pepe").contains("B001-00000005").contains("María Pérez")
                .contains("cajero").contains("17/10/2026 14:30")
                .contains("Arroz extra 1 kg").contains("Azúcar rubia")
                .contains("S/ 3.50").contains("S/ 10.50").contains("TOTAL: S/ 14.50");
        assertThat(texto).doesNotContain("ANULADA");
    }

    @Test
    @DisplayName("Sin cliente se imprime 'Clientes varios'")
    void sinCliente() throws IOException {
        String texto = texto(boleta.generar(venta(EstadoVenta.COMPLETADA, null, detalle())));

        assertThat(texto).contains("Clientes varios");
    }

    @Test
    @DisplayName("Una venta anulada lleva la marca, el motivo y la fecha de anulación")
    void ventaAnulada() throws IOException {
        String texto = texto(boleta.generar(venta(EstadoVenta.ANULADA, "María Pérez", detalle())));

        assertThat(texto).contains("VENTA ANULADA").contains("Error de digitación").contains("17/10/2026 15:00");
    }

    @Test
    @DisplayName("Con muchos productos el PDF sigue siendo válido y continúa en otra página")
    void muchosProductos() throws IOException {
        List<DetalleResponse> muchos = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            muchos.add(new DetalleResponse((long) i, "P-" + i, "Producto número " + i, 1,
                    new BigDecimal("1.00"), new BigDecimal("1.00")));
        }

        byte[] pdf = boleta.generar(venta(EstadoVenta.COMPLETADA, null, muchos));

        PdfReader lector = new PdfReader(pdf);
        try {
            assertThat(lector.getNumberOfPages()).isGreaterThan(1);
        } finally {
            lector.close();
        }
        assertThat(texto(pdf)).contains("Producto número 60").contains("TOTAL");
    }
}
