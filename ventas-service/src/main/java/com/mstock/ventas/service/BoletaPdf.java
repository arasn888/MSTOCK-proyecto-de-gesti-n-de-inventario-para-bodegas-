package com.mstock.ventas.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mstock.ventas.dto.DetalleResponse;
import com.mstock.ventas.dto.VentaResponse;
import com.mstock.ventas.entity.EstadoVenta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Boleta de venta (simulada) en PDF, tamaño A6. No lleva el costo de los productos: solo lo que ve el cliente.
 * Una venta anulada se imprime con la marca "VENTA ANULADA" y su motivo.
 */
@Component
public class BoletaPdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NUMERO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font ANULADA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font PIE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7);

    private final String negocio;

    public BoletaPdf(@Value("${mstock.boleta.negocio:MSTOCK}") String negocio) {
        this.negocio = negocio;
    }

    public byte[] generar(VentaResponse venta) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A6, 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(centrado(negocio, TITULO));
            documento.add(centrado("BOLETA DE VENTA (SIMULADA)", SUBTITULO));
            documento.add(centrado(venta.numero(), NUMERO));
            documento.add(new Paragraph(" ", NORMAL));

            documento.add(new Paragraph("Fecha: " + FECHA.format(venta.fecha()), NORMAL));
            documento.add(new Paragraph("Atendido por: " + venta.usuario(), NORMAL));
            documento.add(new Paragraph("Cliente: "
                    + (venta.clienteNombre() == null || venta.clienteNombre().isBlank()
                    ? "Clientes varios" : venta.clienteNombre()), NORMAL));

            if (venta.estado() == EstadoVenta.ANULADA) {
                documento.add(new Paragraph(" ", NORMAL));
                documento.add(centrado("*** VENTA ANULADA ***", ANULADA));
                if (venta.motivoAnulacion() != null) {
                    documento.add(centrado("Motivo: " + venta.motivoAnulacion(), NORMAL));
                }
                if (venta.fechaAnulacion() != null) {
                    documento.add(centrado("Anulada el " + FECHA.format(venta.fechaAnulacion()), NORMAL));
                }
            }
            documento.add(new Paragraph(" ", NORMAL));

            PdfPTable tabla = new PdfPTable(new float[]{1.1f, 5f, 2f, 2.2f});
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            tabla.addCell(celda("Cant.", NEGRITA, Element.ALIGN_LEFT, Rectangle.BOTTOM));
            tabla.addCell(celda("Descripción", NEGRITA, Element.ALIGN_LEFT, Rectangle.BOTTOM));
            tabla.addCell(celda("P. unit.", NEGRITA, Element.ALIGN_RIGHT, Rectangle.BOTTOM));
            tabla.addCell(celda("Importe", NEGRITA, Element.ALIGN_RIGHT, Rectangle.BOTTOM));
            for (DetalleResponse d : venta.detalle()) {
                tabla.addCell(celda(String.valueOf(d.cantidad()), NORMAL, Element.ALIGN_LEFT, Rectangle.NO_BORDER));
                tabla.addCell(celda(d.nombre(), NORMAL, Element.ALIGN_LEFT, Rectangle.NO_BORDER));
                tabla.addCell(celda(dinero(d.precioUnitario()), NORMAL, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
                tabla.addCell(celda(dinero(d.subtotal()), NORMAL, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
            }
            documento.add(tabla);

            Paragraph total = new Paragraph("TOTAL: " + dinero(venta.total()), TOTAL);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(8);
            documento.add(total);

            Paragraph pie = centrado("Comprobante simulado, sin valor tributario. ¡Gracias por su compra!", PIE);
            pie.setSpacingBefore(14);
            documento.add(pie);

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar la boleta " + venta.numero(), e);
        }
        return salida.toByteArray();
    }

    private static Paragraph centrado(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_CENTER);
        return parrafo;
    }

    private static PdfPCell celda(String texto, Font fuente, int alineacion, int borde) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setBorder(borde);
        celda.setPaddingTop(2);
        celda.setPaddingBottom(2);
        return celda;
    }

    private static String dinero(BigDecimal monto) {
        return "S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
