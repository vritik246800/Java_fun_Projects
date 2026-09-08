package app.ui;

import app.modelo.Paragem;
import app.modelo.Rota;
import app.modelo.Veiculo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

/** Separador de estatísticas: extensão das rotas e composição da frota (JFreeChart). */
public class PainelEstatisticas extends JPanel {

    private final List<Paragem> paragens;
    private final List<Rota> rotas;
    private final List<Veiculo> veiculos;

    public PainelEstatisticas(List<Paragem> paragens, List<Rota> rotas, List<Veiculo> veiculos) {
        super(new BorderLayout(4, 4));
        this.paragens = paragens;
        this.rotas = rotas;
        this.veiculos = veiculos;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        refrescar();
    }

    /** Reconstrói os gráficos a partir do estado actual da rede. */
    public void refrescar() {
        removeAll();

        double totalKm = 0;
        for (Rota r : rotas) {
            totalKm += r.comprimentoKm();
        }
        JLabel resumo = new JLabel(String.format("<html>%d rotas · %d paragens · %d veículos<br>"
                + "Extensão total da rede: <b>%.0f km</b></html>",
                rotas.size(), paragens.size(), veiculos.size(), totalKm));
        resumo.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 4));

        JPanel graficos = new JPanel(new GridLayout(2, 1, 4, 8));
        graficos.add(new ChartPanel(graficoExtensaoRotas()));
        graficos.add(new ChartPanel(graficoFrotaPorTipo()));

        add(resumo, BorderLayout.NORTH);
        add(graficos, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** Refaz os gráficos quando o tema muda, para acompanharem as cores do Look & Feel. */
    @Override
    public void updateUI() {
        super.updateUI();
        if (rotas != null) { // chamado também pelo construtor da superclasse, antes dos campos
            SwingUtilities.invokeLater(this::refrescar);
        }
    }

    private JFreeChart graficoExtensaoRotas() {
        DefaultCategoryDataset dados = new DefaultCategoryDataset();
        List<Color> cores = new ArrayList<>();
        for (Rota r : rotas) {
            dados.addValue(r.comprimentoKm(), "km", r.getNome());
            cores.add(r.getCor());
        }
        JFreeChart grafico = ChartFactory.createBarChart("Extensão das rotas (km)", null, null,
                dados, PlotOrientation.VERTICAL, false, true, false);
        CategoryPlot plot = grafico.getCategoryPlot();
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int linha, int coluna) {
                return coluna < cores.size() ? cores.get(coluna) : Color.GRAY;
            }
        };
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        plot.setRenderer(renderer);
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        plot.setDomainGridlinesVisible(false);
        aplicarTema(grafico, plot);
        return grafico;
    }

    private JFreeChart graficoFrotaPorTipo() {
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        for (Veiculo v : veiculos) {
            porTipo.merge(v.getTipo(), 1, Integer::sum);
        }
        DefaultPieDataset<String> dados = new DefaultPieDataset<>();
        porTipo.forEach(dados::setValue);
        JFreeChart grafico = ChartFactory.createPieChart("Frota por tipo", dados, true, true, false);
        PiePlot<?> plot = (PiePlot<?>) grafico.getPlot();
        plot.setLabelGenerator(null); // rótulos só na legenda, sobram poucos pixéis
        plot.setCircular(true);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null); // a sombra do JFreeChart destoa de um tema flat
        aplicarTema(grafico, plot);
        return grafico;
    }

    /** Alinha fundo, letra e cor do texto do gráfico com o Look & Feel activo. */
    private static void aplicarTema(JFreeChart grafico, Plot plot) {
        Color fundo = UIManager.getColor("Panel.background");
        Color texto = UIManager.getColor("Label.foreground");
        Font base = UIManager.getFont("Label.font");
        grafico.setBackgroundPaint(fundo);
        plot.setBackgroundPaint(fundo);
        plot.setOutlinePaint(null);
        if (grafico.getTitle() != null) {
            grafico.getTitle().setPaint(texto);
            grafico.getTitle().setFont(base.deriveFont(Font.BOLD, base.getSize() + 1f));
        }
        if (grafico.getLegend() != null) {
            grafico.getLegend().setBackgroundPaint(fundo);
            grafico.getLegend().setItemPaint(texto);
            grafico.getLegend().setItemFont(base);
            grafico.getLegend().setBorder(0, 0, 0, 0);
        }
        if (plot instanceof CategoryPlot cp) {
            cp.getDomainAxis().setTickLabelPaint(texto);
            cp.getDomainAxis().setTickLabelFont(base.deriveFont(base.getSize() - 1f));
            cp.getRangeAxis().setTickLabelPaint(texto);
            cp.getRangeAxis().setTickLabelFont(base.deriveFont(base.getSize() - 1f));
            cp.setRangeGridlinePaint(new Color(texto.getRed(), texto.getGreen(), texto.getBlue(), 50));
        }
    }
}
