package utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import modelo.Produto;

public class PDFUtils {

    // =============================
    // 1) INVENTÁRIO (já tinhas)
    // =============================
    public static void gerarInventario(List<Produto> produtos) throws Exception {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, new FileOutputStream("inventario.pdf"));
        doc.open();

        doc.add(new Paragraph("INVENTÁRIO DA LOJA"));
        doc.add(new Paragraph(" "));

        Table t = new Table(4);
        t.addCell("Nome");
        t.addCell("Preço Custo");
        t.addCell("Margem");
        t.addCell("Stock");

        for (Produto p : produtos) {
            t.addCell(p.getNome());
            t.addCell("" + p.getPrecoCusto());
            t.addCell("" + p.getMargemLucro());
            t.addCell("" + p.getStock());
        }

        doc.add(t);
        doc.close();
    }

    // =============================
    // 2) RECIBO DE VENDA (texto)
    // =============================
    public static void gerarPDFRecibo(String conteudo, String nomeArquivo) throws Exception {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
        doc.open();

        doc.add(new Paragraph("RECIBO DE VENDA"));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(conteudo));

        doc.close();
    }

    // =============================
    // 3) PDF TEXTO SIMPLES
    // =============================
    public static void gerarPDFTexto(String nomeArquivo, String conteudo) throws Exception {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
        doc.open();
        doc.add(new Paragraph(conteudo));
        doc.close();
    }

    // =============================
    // 4) PDF COM GRÁFICO (imagem)
    // =============================
    public static void gerarPDFComGrafico(BufferedImage grafico, String nomeArquivo) throws Exception {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
        doc.open();

        // Converter BufferedImage para Image iText
        com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(grafico, null);
        img.scaleToFit(500, 500);   // diminuir para caber no PDF
        doc.add(new Paragraph("Gráfico Gerado:"));
        doc.add(new Paragraph(" "));
        doc.add(img);

        doc.close();
    }
}
