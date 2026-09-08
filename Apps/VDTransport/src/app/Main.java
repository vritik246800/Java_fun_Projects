package app;

import app.bd.BaseDados;
import app.mapa.PainelMapa;
import app.modelo.Paragem;
import app.modelo.Rota;
import app.modelo.Veiculo;
import app.relatorio.RelatorioPdf;
import app.sim.Simulador;
import app.ui.PainelGestao;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/** Gestão de transportes públicos de Moçambique — mapa, rotas, paragens e veículos animados. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        try {
            BaseDados.inicializar();
            List<Paragem> paragens = BaseDados.carregarParagens();
            List<Rota> rotas = BaseDados.carregarRotas();
            List<Veiculo> veiculos = BaseDados.carregarVeiculos(rotas);
            SwingUtilities.invokeLater(() -> criarJanela(paragens, rotas, veiculos));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Falha ao abrir a base de dados:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void criarJanela(List<Paragem> paragens, List<Rota> rotas, List<Veiculo> veiculos) {
        PainelMapa mapa = new PainelMapa(rotas, paragens, veiculos);
        Simulador simulador = new Simulador(veiculos, mapa);
        PainelGestao gestao = new PainelGestao(paragens, rotas, veiculos, mapa);
        gestao.setMinimumSize(new Dimension(260, 0));

        JToolBar barra = new JToolBar();
        barra.setFloatable(false);

        JButton aproximar = new JButton(FontIcon.of(FontAwesomeSolid.SEARCH_PLUS, 16));
        aproximar.setToolTipText("Aproximar");
        aproximar.addActionListener(e -> mapa.zoomMais());

        JButton afastar = new JButton(FontIcon.of(FontAwesomeSolid.SEARCH_MINUS, 16));
        afastar.setToolTipText("Afastar");
        afastar.addActionListener(e -> mapa.zoomMenos());

        JButton enquadrar = new JButton(FontIcon.of(FontAwesomeSolid.GLOBE_AFRICA, 16));
        enquadrar.setToolTipText("Ver Moçambique inteiro");
        enquadrar.addActionListener(e -> mapa.enquadrar());

        JToggleButton play = new JToggleButton(FontIcon.of(FontAwesomeSolid.PAUSE, 16), true);
        play.setToolTipText("Pausar / retomar a simulação");
        play.addActionListener(e -> {
            if (play.isSelected()) {
                simulador.iniciar();
                play.setIcon(FontIcon.of(FontAwesomeSolid.PAUSE, 16));
            } else {
                simulador.parar();
                play.setIcon(FontIcon.of(FontAwesomeSolid.PLAY, 16));
            }
        });

        JLabel lblVelocidade = new JLabel(FontIcon.of(FontAwesomeSolid.TACHOMETER_ALT, 16));
        lblVelocidade.setToolTipText("Velocidade da simulação");
        JSlider slider = new JSlider(200, 4000, simulador.getEscalaTempo());
        slider.setPreferredSize(new Dimension(140, slider.getPreferredSize().height));
        JLabel lblEscala = new JLabel(simulador.getEscalaTempo() + "×");
        slider.addChangeListener(e -> {
            simulador.setEscalaTempo(slider.getValue());
            lblEscala.setText(slider.getValue() + "×");
        });

        JButton pdf = new JButton(FontIcon.of(FontAwesomeSolid.FILE_PDF, 16));
        pdf.setToolTipText("Exportar relatório da rede em PDF");
        pdf.addActionListener(e -> exportarPdf(pdf, paragens, rotas, veiculos));

        JToggleButton tema = new JToggleButton(FontIcon.of(FontAwesomeSolid.MOON, 16));
        tema.setToolTipText("Alternar tema claro / escuro");
        tema.addActionListener(e -> {
            if (tema.isSelected()) {
                FlatDarkLaf.setup();
                tema.setIcon(FontIcon.of(FontAwesomeSolid.SUN, 16));
            } else {
                FlatLightLaf.setup();
                tema.setIcon(FontIcon.of(FontAwesomeSolid.MOON, 16));
            }
            FlatLaf.updateUI();
        });

        barra.add(aproximar);
        barra.add(afastar);
        barra.add(enquadrar);
        barra.addSeparator();
        barra.add(play);
        barra.addSeparator();
        barra.add(lblVelocidade);
        barra.add(slider);
        barra.add(lblEscala);
        barra.addSeparator();
        barra.add(pdf);
        barra.add(tema);

        JLabel estado = new JLabel(" SQLite · transportes.db · mapa © OpenStreetMap",
                FontIcon.of(FontAwesomeSolid.DATABASE, 12), SwingConstants.LEFT);

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, gestao, mapa);
        divisao.setDividerLocation(320);
        divisao.setResizeWeight(0); // ao redimensionar a janela cresce o mapa, não o painel lateral

        JFrame janela = new JFrame("Gestão de Transportes Públicos — Moçambique");
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setLayout(new BorderLayout());
        janela.add(barra, BorderLayout.NORTH);
        janela.add(divisao, BorderLayout.CENTER);
        janela.add(estado, BorderLayout.SOUTH);
        janela.setSize(1280, 800);
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);

        mapa.enquadrar();
        simulador.iniciar();
    }

    private static void exportarPdf(Component pai, List<Paragem> paragens, List<Rota> rotas,
            List<Veiculo> veiculos) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar relatório PDF");
        selector.setSelectedFile(new File("relatorio-transportes.pdf"));
        if (selector.showSaveDialog(pai) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File ficheiro = selector.getSelectedFile();
        try {
            RelatorioPdf.gerar(ficheiro, paragens, rotas, veiculos);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(ficheiro);
            } else {
                JOptionPane.showMessageDialog(pai, "Relatório guardado em:\n" + ficheiro.getAbsolutePath(),
                        "Relatório", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(pai, "Não foi possível gerar o relatório:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
