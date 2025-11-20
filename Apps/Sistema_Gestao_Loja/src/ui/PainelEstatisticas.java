package ui;

import database.VendaDAO;
import modelo.Cliente;
import modelo.Produto;
import utils.PDFUtils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

public class PainelEstatisticas extends JPanel {

    private JFreeChart chartVendasDia;
    private JFreeChart chartVendasMes;
    private JFreeChart chartProdutoMaisVendido;

    public PainelEstatisticas(List<Produto> produtos, List<Cliente> clientes) {
        setLayout(new BorderLayout());

        // Topo: resumo rápido
        JPanel resumo = new JPanel(new GridLayout(1, 3));
        resumo.add(labelCard("Produtos", String.valueOf(produtos.size())));
        resumo.add(labelCard("Clientes", String.valueOf(clientes.size())));
        resumo.add(labelCard("Valor Stock (est.)", String.valueOf(calculaValorStock(produtos))));
        add(resumo, BorderLayout.NORTH);

        // Centro: gráficos
        JPanel centro = new JPanel(new GridLayout(2, 2));

        // Vendas por dia (barra)
        DefaultCategoryDataset dsDia = new DefaultCategoryDataset();
        try {
            for (Map<String, Object> row : VendaDAO.vendasPorDia()) {
                dsDia.addValue(((Number) row.get("total")).doubleValue(), "Vendas", (String) row.get("dia"));
            }
        } catch (Exception ex) { ex.printStackTrace(); }

        chartVendasDia = ChartFactory.createBarChart("Vendas por Dia", "Dia", "Valor (MT)", dsDia);
        centro.add(new ChartPanel(chartVendasDia));

        // Vendas por mês (linha)
        DefaultCategoryDataset dsMes = new DefaultCategoryDataset();
        try {
            for (Map<String, Object> row : VendaDAO.vendasPorMes()) {
                dsMes.addValue(((Number) row.get("total")).doubleValue(), "Vendas", (String) row.get("mes"));
            }
        } catch (Exception ex) { ex.printStackTrace(); }

        chartVendasMes = ChartFactory.createLineChart("Vendas por Mês", "Mês", "Valor (MT)", dsMes);
        centro.add(new ChartPanel(chartVendasMes));

        // Produto mais vendido (pizza)
        DefaultPieDataset dsProd = new DefaultPieDataset();
        try {
            for (Map<String, Object> row : VendaDAO.produtoMaisVendido(10)) {
                dsProd.setValue((String) row.get("nome"), ((Number) row.get("qtd")).intValue());
            }
        } catch (Exception ex) { ex.printStackTrace(); }

        chartProdutoMaisVendido = ChartFactory.createPieChart("Produtos mais vendidos", dsProd, true, true, false);
        centro.add(new ChartPanel(chartProdutoMaisVendido));

        // Espaço vazio ou futuro gráfico
        centro.add(new JPanel());

        add(centro, BorderLayout.CENTER);

        // Rodapé: botões de export
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnExportDia = new JButton("Exportar Vendas por Dia (PDF)");
        JButton btnExportMes = new JButton("Exportar Vendas por Mês (PDF)");
        JButton btnExportProd = new JButton("Exportar Produtos (PDF)");

        btnExportDia.addActionListener(e -> exportChartToPDF(chartVendasDia, "vendas_dia.pdf"));
        btnExportMes.addActionListener(e -> exportChartToPDF(chartVendasMes, "vendas_mes.pdf"));
        btnExportProd.addActionListener(e -> exportChartToPDF(chartProdutoMaisVendido, "produtos_mais_vendidos.pdf"));

        rodape.add(btnExportProd);
        rodape.add(btnExportDia);
        rodape.add(btnExportMes);

        add(rodape, BorderLayout.SOUTH);
    }

    private JPanel labelCard(String title, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        JLabel l = new JLabel(value, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 20));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private double calculaValorStock(List<Produto> produtos) {
        double total = 0;
        for (Produto p : produtos) total += p.getPrecoFinal() * p.getStock();
        return total;
    }

    private void exportChartToPDF(JFreeChart chart, String filename) {
        try {
            BufferedImage img = chart.createBufferedImage(800, 600);
            PDFUtils.gerarPDFComGrafico(img, filename);
            java.awt.Desktop.getDesktop().open(new File(filename));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro exportar PDF: " + ex.getMessage());
        }
    }
}
