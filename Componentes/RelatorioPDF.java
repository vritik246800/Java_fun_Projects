import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import models.Grafo;
import models.No;
import algoritms.ResultadoTSP;
import algoritms.ResultadoDijkstra;
import util.UtilGrafo;
import util.ConversorTempo;

public class RelatorioPDF {

    private String titulo;
    private List<String> historico;
    private String resultado;

    // Novos campos para análise avançada
    private Grafo grafo;
    private ResultadoTSP resultadoTSP;
    private ResultadoDijkstra resultadoDijkstra;

    public RelatorioPDF(String titulo, List<String> historico, String resultado) {
        this.titulo = titulo;
        this.historico = historico;
        this.resultado = resultado;
        this.grafo = null;
        this.resultadoTSP = null;
        this.resultadoDijkstra = null;
    }

    // Construtor expandido com dados de análise
    public RelatorioPDF(String titulo, List<String> historico, String resultado,
                       Grafo grafo, ResultadoTSP resultadoTSP, ResultadoDijkstra resultadoDijkstra) {
        this.titulo = titulo;
        this.historico = historico;
        this.resultado = resultado;
        this.grafo = grafo;
        this.resultadoTSP = resultadoTSP;
        this.resultadoDijkstra = resultadoDijkstra;
    }

    public void gerarPDF(Component parent) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Escolher diretório para guardar o relatório");
            fileChooser.setSelectedFile(new File("relatorio_logistica_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")) + ".pdf"));

            int userSelection = fileChooser.showSaveDialog(parent);
            if (userSelection != JFileChooser.APPROVE_OPTION) return;

            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
            }

            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
            document.open();

            // 🎨 Cores e fontes
            Color azul = new Color(33, 150, 243);
            Color dourado = new Color(255, 215, 64);
            Color cinzaClaro = new Color(245, 245, 245);

            Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD, azul);
            Font dataFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font secaoFont = new Font(Font.HELVETICA, 13, Font.BOLD, dourado);
            Font corpoFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
            Font rodapeFont = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);

            // Logotipo removido para simplificar o relatório

            // 🔷 Cabeçalho
            Paragraph cabecalho = new Paragraph(titulo.toUpperCase(), tituloFont);
            cabecalho.setAlignment(Element.ALIGN_CENTER);
            cabecalho.setSpacingAfter(5);
            document.add(cabecalho);

            Paragraph data = new Paragraph("Gerado em: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    dataFont);
            data.setAlignment(Element.ALIGN_CENTER);
            document.add(data);
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Resultado da Operação
            if (resultado != null && !resultado.isEmpty()) {
                Paragraph secao2 = new Paragraph("RESULTADO DA OPERAÇÃO", secaoFont);
                secao2.setAlignment(Element.ALIGN_CENTER);
                secao2.setSpacingBefore(10);
                secao2.setSpacingAfter(10);
                document.add(secao2);

                Paragraph res = new Paragraph(resultado, corpoFont);
                res.setAlignment(Element.ALIGN_LEFT);
                res.setSpacingBefore(5);
                res.setSpacingAfter(10);
                document.add(res);
                document.add(Chunk.NEWLINE);
            }

            // Análise Detalhada (apenas se houver dados de grafo)
            if (grafo != null) {
                adicionarResumoExecutivo(document, secaoFont, corpoFont);
                adicionarEstatisticasGrafo(document, secaoFont, corpoFont);
            }

            // ✍️ Rodapé
            Paragraph fim = new Paragraph("Relatório gerado automaticamente pelo Sistema de Logística da Ucrânia", rodapeFont);
            fim.setAlignment(Element.ALIGN_CENTER);
            fim.setSpacingBefore(20);
            document.add(fim);

            document.close();

            JOptionPane.showMessageDialog(parent,
                    "✅ Relatório PDF exportado com sucesso!\n\n📁 Ficheiro: " + fileToSave.getAbsolutePath(),
                    "Exportação Concluída",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (DocumentException | IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "❌ Erro ao exportar relatório:\n" + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Adiciona resumo executivo ao PDF
     */
    private void adicionarResumoExecutivo(Document document, Font secaoFont, Font corpoFont) throws DocumentException {
        Paragraph secao = new Paragraph("RESUMO EXECUTIVO", secaoFont);
        secao.setAlignment(Element.ALIGN_CENTER);
        secao.setSpacingBefore(15);
        secao.setSpacingAfter(10);
        document.add(secao);

        int totalNos = grafo.getNos().size();
        int totalArestas = 0;
        for (String key : grafo.getListasAdj().keySet()) {
            totalArestas += grafo.getListasAdj().get(key).size();
        }
        totalArestas /= 2; // Grafo não direcionado

        StringBuilder resumo = new StringBuilder();
        resumo.append("Cidades: ").append(totalNos).append("\n");
        resumo.append("Conexões: ").append(totalArestas).append("\n\n");

        if (resultadoTSP != null) {
            resumo.append("ROTA COMPLETA (TSP - Vizinho Mais Próximo):\n");
            resumo.append("  - Distância total: ").append(resultadoTSP.getCustoTotal()).append(" km\n");
            resumo.append("  - Tempo estimado: ").append(ConversorTempo.pesoParaTempo(resultadoTSP.getCustoTotal())).append("\n");
            resumo.append("  - Cidades visitadas: ").append(resultadoTSP.getRota().size()).append("\n\n");
        }

        if (resultadoDijkstra != null && !resultadoDijkstra.isInalcancavel()) {
            resumo.append("CAMINHO MAIS CURTO (Dijkstra):\n");
            resumo.append("  - Distância: ").append(resultadoDijkstra.getCusto()).append(" km\n");
            resumo.append("  - Tempo: ").append(ConversorTempo.pesoParaTempo(resultadoDijkstra.getCusto())).append("\n");
        }

        Paragraph texto = new Paragraph(resumo.toString(), corpoFont);
        texto.setAlignment(Element.ALIGN_LEFT);
        texto.setSpacingBefore(5);
        texto.setLeading(16f);
        document.add(texto);

        document.add(Chunk.NEWLINE);
    }

    /**
     * Adiciona estatísticas do grafo ao PDF
     */
    private void adicionarEstatisticasGrafo(Document document, Font secaoFont, Font corpoFont) throws DocumentException {
        Paragraph secao = new Paragraph("ANÁLISE DO GRAFO", secaoFont);
        secao.setAlignment(Element.ALIGN_CENTER);
        secao.setSpacingBefore(15);
        secao.setSpacingAfter(10);
        document.add(secao);

        double grauMedio = UtilGrafo.calcularGrauMedio(grafo);
        boolean conexo = UtilGrafo.verificarConectividade(grafo);
        List<No> nosCriticos = UtilGrafo.identificarNosCriticos(grafo);

        StringBuilder stats = new StringBuilder();
        stats.append("Grau médio: ").append(String.format("%.2f", grauMedio)).append("\n");
        stats.append("Conectividade: ").append(conexo ? "Conexo" : "Desconexo").append("\n");
        stats.append("Pontos críticos: ").append(nosCriticos.size()).append("\n");

        if (!nosCriticos.isEmpty()) {
            stats.append("\nCidades críticas (se removidas, desconectam o grafo):\n");
            for (No no : nosCriticos) {
                stats.append("  - ").append(no.getNome()).append("\n");
            }
        }

        Paragraph texto = new Paragraph(stats.toString(), corpoFont);
        texto.setAlignment(Element.ALIGN_LEFT);
        texto.setSpacingBefore(5);
        texto.setLeading(16f);
        document.add(texto);

        document.add(Chunk.NEWLINE);
    }

}
