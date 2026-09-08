package app.relatorio;

import app.modelo.Paragem;
import app.modelo.Rota;
import app.modelo.Veiculo;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Exporta a rede (rotas, paragens e veículos) para um relatório PDF, via OpenPDF. */
public final class RelatorioPdf {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font CABECALHO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Color COR_CABECALHO = new Color(0x37474F);
    private static final Color COR_LINHA_PAR = new Color(0xF2F4F5);

    private RelatorioPdf() {
    }

    public static void gerar(File ficheiro, List<Paragem> paragens, List<Rota> rotas, List<Veiculo> veiculos)
            throws IOException, DocumentException {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try (OutputStream saida = new FileOutputStream(ficheiro)) {
            PdfWriter.getInstance(doc, saida);
            doc.open();

            doc.add(new Paragraph("Rede de Transportes Públicos — Moçambique", TITULO));
            doc.add(new Paragraph("Gerado em "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), NORMAL));
            doc.add(espaco());

            doc.add(new Paragraph("Rotas (" + rotas.size() + ")", SUBTITULO));
            PdfPTable tabelaRotas = tabela(new float[]{4f, 1.3f, 1.7f}, "Rota", "Paragens", "Extensão (km)");
            double totalKm = 0;
            for (Rota r : rotas) {
                totalKm += r.comprimentoKm();
                celula(tabelaRotas, r.getNome());
                celula(tabelaRotas, String.valueOf(r.getParagens().size()));
                celula(tabelaRotas, String.format("%.1f", r.comprimentoKm()));
            }
            doc.add(tabelaRotas);
            doc.add(new Paragraph(String.format("Extensão total da rede: %.1f km", totalKm), NORMAL));
            doc.add(espaco());

            doc.add(new Paragraph("Paragens (" + paragens.size() + ")", SUBTITULO));
            PdfPTable tabelaParagens = tabela(new float[]{4f, 1.5f, 1.5f}, "Paragem", "Latitude", "Longitude");
            for (Paragem p : paragens) {
                celula(tabelaParagens, p.getNome());
                celula(tabelaParagens, String.format("%.4f", p.getLatitude()));
                celula(tabelaParagens, String.format("%.4f", p.getLongitude()));
            }
            doc.add(tabelaParagens);
            doc.add(espaco());

            doc.add(new Paragraph("Veículos (" + veiculos.size() + ")", SUBTITULO));
            PdfPTable tabelaVeiculos = tabela(new float[]{1.6f, 1.6f, 3.5f, 1.6f},
                    "Matrícula", "Tipo", "Rota", "Velocidade (km/h)");
            for (Veiculo v : veiculos) {
                celula(tabelaVeiculos, v.getMatricula());
                celula(tabelaVeiculos, v.getTipo());
                celula(tabelaVeiculos, v.getRota() != null ? v.getRota().getNome() : "—");
                celula(tabelaVeiculos, String.format("%.0f", v.getVelocidadeKmh()));
            }
            doc.add(tabelaVeiculos);

            doc.close();
        }
    }

    private static Paragraph espaco() {
        return new Paragraph(" ", NORMAL);
    }

    private static PdfPTable tabela(float[] larguras, String... cabecalhos) throws DocumentException {
        PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(6);
        tabela.setHeaderRows(1);
        for (String texto : cabecalhos) {
            PdfPCell celula = new PdfPCell(new Phrase(texto, CABECALHO));
            celula.setBackgroundColor(COR_CABECALHO);
            celula.setPadding(5);
            celula.setBorderColor(COR_CABECALHO);
            tabela.addCell(celula);
        }
        return tabela;
    }

    private static void celula(PdfPTable tabela, String texto) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, NORMAL));
        celula.setPadding(4);
        celula.setBorderColor(Color.LIGHT_GRAY);
        celula.setHorizontalAlignment(Element.ALIGN_LEFT);
        if (tabela.getRows().size() % 2 == 0) {
            celula.setBackgroundColor(COR_LINHA_PAR);
        }
        tabela.addCell(celula);
    }
}
