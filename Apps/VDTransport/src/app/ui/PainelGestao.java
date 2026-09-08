package app.ui;

import app.bd.BaseDados;
import app.mapa.PainelMapa;
import app.modelo.Paragem;
import app.modelo.Rota;
import app.modelo.Veiculo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import org.jxmapviewer.viewer.GeoPosition;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/** Painel lateral com a gestão de rotas, paragens e veículos (CRUD sobre SQLite). */
public class PainelGestao extends JTabbedPane {

    private final List<Paragem> paragens;
    private final List<Rota> rotas;
    private final List<Veiculo> veiculos;
    private final PainelMapa mapa;

    private final DefaultListModel<Rota> modeloRotas = new DefaultListModel<>();
    private final DefaultListModel<Paragem> modeloParagens = new DefaultListModel<>();
    private final DefaultListModel<Paragem> modeloParagensRota = new DefaultListModel<>();
    private final DefaultListModel<Veiculo> modeloVeiculos = new DefaultListModel<>();

    private final JList<Rota> listaRotas = new JList<>(modeloRotas);
    private final JList<Paragem> listaParagens = new JList<>(modeloParagens);
    private final JList<Paragem> listaParagensRota = new JList<>(modeloParagensRota);
    private final JList<Veiculo> listaVeiculos = new JList<>(modeloVeiculos);
    private final JComboBox<Paragem> cmbParagens = new JComboBox<>();

    public PainelGestao(List<Paragem> paragens, List<Rota> rotas, List<Veiculo> veiculos, PainelMapa mapa) {
        this.paragens = paragens;
        this.rotas = rotas;
        this.veiculos = veiculos;
        this.mapa = mapa;

        configurarListas();
        addTab("Rotas", FontIcon.of(FontAwesomeSolid.ROUTE, 14), criarTabRotas());
        addTab("Paragens", FontIcon.of(FontAwesomeSolid.MAP_MARKER_ALT, 14), criarTabParagens());
        addTab("Veículos", FontIcon.of(FontAwesomeSolid.BUS, 14), criarTabVeiculos());
        addChangeListener(e -> refrescar());
        refrescar();
    }

    private void configurarListas() {
        listaRotas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaRotas.setCellRenderer((lista, rota, i, sel, foco) ->
                celula(lista, rota.getNome(), FontIcon.of(FontAwesomeSolid.ROUTE, 14, rota.getCor()), sel));
        listaRotas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mapa.setRotaSelecionada(listaRotas.getSelectedValue());
                refrescarParagensRota();
            }
        });

        listaParagens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaParagens.setCellRenderer((lista, p, i, sel, foco) ->
                celula(lista, p.getNome(),
                        FontIcon.of(FontAwesomeSolid.MAP_MARKER_ALT, 14, new Color(0x616161)), sel));
        listaParagensRota.setCellRenderer(listaParagens.getCellRenderer());

        listaVeiculos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaVeiculos.setCellRenderer((lista, v, i, sel, foco) ->
                celula(lista, v.toString(), iconeDe(v.getTipo(),
                        v.getRota() != null ? v.getRota().getCor() : new Color(0x616161)), sel));
    }

    // ---------------- construção dos separadores ----------------

    private Component criarTabRotas() {
        JButton nova = botao("Nova rota", FontAwesomeSolid.PLUS);
        nova.addActionListener(e -> novaRota());
        JButton apagar = botao("Apagar", FontAwesomeSolid.TRASH_ALT);
        apagar.addActionListener(e -> apagarRota());
        JPanel accoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        accoes.add(nova);
        accoes.add(apagar);

        JPanel topo = new JPanel(new BorderLayout(4, 4));
        topo.add(new JLabel("Rotas"), BorderLayout.NORTH);
        topo.add(new JScrollPane(listaRotas), BorderLayout.CENTER);
        topo.add(accoes, BorderLayout.SOUTH);

        JButton adicionar = botao("Adicionar paragem", FontAwesomeSolid.PLUS);
        adicionar.addActionListener(e -> adicionarParagemARota());
        JButton remover = botao("Remover paragem", FontAwesomeSolid.TIMES);
        remover.addActionListener(e -> removerParagemDaRota());
        JPanel botoesParagem = new JPanel(new GridLayout(1, 2, 4, 0));
        botoesParagem.add(adicionar);
        botoesParagem.add(remover);

        JPanel form = new JPanel(new BorderLayout(4, 4));
        form.add(cmbParagens, BorderLayout.CENTER);
        form.add(botoesParagem, BorderLayout.SOUTH);

        JPanel baixo = new JPanel(new BorderLayout(4, 4));
        baixo.add(new JLabel("Paragens da rota seleccionada (por ordem)"), BorderLayout.NORTH);
        baixo.add(new JScrollPane(listaParagensRota), BorderLayout.CENTER);
        baixo.add(form, BorderLayout.SOUTH);

        JSplitPane divisao = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, topo, baixo);
        divisao.setResizeWeight(0.5);
        divisao.setBorder(null);

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        painel.add(divisao, BorderLayout.CENTER);
        return painel;
    }

    private Component criarTabParagens() {
        JToggleButton noMapa = new JToggleButton("Adicionar no mapa");
        noMapa.setIcon(FontIcon.of(FontAwesomeSolid.LOCATION_ARROW, 14));
        noMapa.setToolTipText("Activa o modo em que cada clique no mapa cria uma paragem");
        noMapa.addActionListener(e -> {
            if (noMapa.isSelected()) {
                mapa.setModoAdicionarParagem(this::adicionarParagemNoMapa);
            } else {
                mapa.setModoAdicionarParagem(null);
            }
        });
        JButton apagar = botao("Apagar", FontAwesomeSolid.TRASH_ALT);
        apagar.addActionListener(e -> apagarParagem());

        JPanel accoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        accoes.add(noMapa);
        accoes.add(apagar);

        JLabel dica = new JLabel("Com o modo activo, clique no mapa para criar a paragem.");
        dica.setFont(dica.getFont().deriveFont(11f));

        JPanel sul = new JPanel(new BorderLayout(4, 4));
        sul.add(accoes, BorderLayout.NORTH);
        sul.add(dica, BorderLayout.SOUTH);

        JPanel painel = new JPanel(new BorderLayout(4, 4));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        painel.add(new JScrollPane(listaParagens), BorderLayout.CENTER);
        painel.add(sul, BorderLayout.SOUTH);
        return painel;
    }

    private Component criarTabVeiculos() {
        JButton novo = botao("Novo veículo", FontAwesomeSolid.PLUS);
        novo.addActionListener(e -> novoVeiculo());
        JButton apagar = botao("Apagar", FontAwesomeSolid.TRASH_ALT);
        apagar.addActionListener(e -> apagarVeiculo());

        JPanel accoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        accoes.add(novo);
        accoes.add(apagar);

        JPanel painel = new JPanel(new BorderLayout(4, 4));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        painel.add(new JScrollPane(listaVeiculos), BorderLayout.CENTER);
        painel.add(accoes, BorderLayout.SOUTH);
        return painel;
    }

    // ---------------- acções ----------------

    private void novaRota() {
        String nome = JOptionPane.showInputDialog(this, "Nome da rota:", "Nova rota",
                JOptionPane.PLAIN_MESSAGE);
        if (nome == null || nome.isBlank()) {
            return;
        }
        Color cor = JColorChooser.showDialog(this, "Cor da rota", new Color(0xD32F2F));
        if (cor == null) {
            return;
        }
        try {
            Rota rota = BaseDados.inserirRota(nome.trim(),
                    String.format("#%02X%02X%02X", cor.getRed(), cor.getGreen(), cor.getBlue()));
            rotas.add(rota);
            refrescar();
            listaRotas.setSelectedValue(rota, true);
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    private void apagarRota() {
        Rota r = listaRotas.getSelectedValue();
        if (r == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Apagar a rota \"" + r.getNome() + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            BaseDados.apagarRota(r.getId());
            rotas.remove(r);
            for (Veiculo v : veiculos) {
                if (v.getRota() == r) {
                    v.setRota(null); // ON DELETE SET NULL na base
                }
            }
            mapa.setRotaSelecionada(null);
            refrescar();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    private void adicionarParagemARota() {
        Rota r = listaRotas.getSelectedValue();
        Paragem p = (Paragem) cmbParagens.getSelectedItem();
        if (r == null || p == null) {
            info("Seleccione uma rota e uma paragem.");
            return;
        }
        if (r.getParagens().contains(p)) {
            info("A paragem já pertence a esta rota.");
            return;
        }
        try {
            BaseDados.adicionarParagemARota(r.getId(), p.getId());
            r.getParagens().add(p);
            refrescarParagensRota();
            mapa.repaint();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    private void removerParagemDaRota() {
        Rota r = listaRotas.getSelectedValue();
        Paragem p = listaParagensRota.getSelectedValue();
        if (r == null || p == null) {
            info("Seleccione a paragem a remover da rota.");
            return;
        }
        try {
            BaseDados.removerParagemDaRota(r.getId(), p.getId());
            r.getParagens().remove(p);
            refrescarParagensRota();
            mapa.repaint();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    private void adicionarParagemNoMapa(GeoPosition geo) {
        String nome = JOptionPane.showInputDialog(this, "Nome da paragem:", "Nova paragem",
                JOptionPane.PLAIN_MESSAGE);
        if (nome == null || nome.isBlank()) {
            return;
        }
        try {
            Paragem p = BaseDados.inserirParagem(nome.trim(), geo.getLatitude(), geo.getLongitude());
            paragens.add(p);
            refrescar();
            mapa.repaint();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    private void apagarParagem() {
        Paragem p = listaParagens.getSelectedValue();
        if (p == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Apagar a paragem \"" + p.getNome() + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            BaseDados.apagarParagem(p.getId());
            paragens.remove(p);
            for (Rota r : rotas) {
                r.getParagens().remove(p); // ON DELETE CASCADE na base
            }
            refrescar();
            mapa.repaint();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    private void novoVeiculo() {
        JTextField txtMatricula = new JTextField(10);
        JComboBox<String> cmbTipo = new JComboBox<>(
                new String[]{"Autocarro", "Machimbombo", "Chapa", "Comboio"});
        DefaultComboBoxModel<Rota> modeloRota = new DefaultComboBoxModel<>();
        modeloRota.addElement(null); // sem rota
        rotas.forEach(modeloRota::addElement);
        JComboBox<Rota> cmbRota = new JComboBox<>(modeloRota);
        JSpinner spnVelocidade = new JSpinner(new SpinnerNumberModel(60, 10, 160, 5));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Matrícula:"));
        form.add(txtMatricula);
        form.add(new JLabel("Tipo:"));
        form.add(cmbTipo);
        form.add(new JLabel("Rota:"));
        form.add(cmbRota);
        form.add(new JLabel("Velocidade (km/h):"));
        form.add(spnVelocidade);

        if (JOptionPane.showConfirmDialog(this, form, "Novo veículo",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        String matricula = txtMatricula.getText().trim();
        if (matricula.isEmpty()) {
            info("Indique a matrícula do veículo.");
            return;
        }
        try {
            Veiculo v = BaseDados.inserirVeiculo(matricula, (String) cmbTipo.getSelectedItem(),
                    (Rota) cmbRota.getSelectedItem(),
                    ((Number) spnVelocidade.getValue()).doubleValue());
            veiculos.add(v);
            refrescar();
            mapa.repaint();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    private void apagarVeiculo() {
        Veiculo v = listaVeiculos.getSelectedValue();
        if (v == null) {
            return;
        }
        try {
            BaseDados.apagarVeiculo(v.getId());
            veiculos.remove(v);
            refrescar();
            mapa.repaint();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    // ---------------- utilidades ----------------

    private void refrescar() {
        int seleccionada = listaRotas.getSelectedIndex();
        modeloRotas.clear();
        rotas.forEach(modeloRotas::addElement);
        if (seleccionada >= 0 && seleccionada < modeloRotas.getSize()) {
            listaRotas.setSelectedIndex(seleccionada);
        }
        modeloParagens.clear();
        paragens.forEach(modeloParagens::addElement);
        modeloVeiculos.clear();
        veiculos.forEach(modeloVeiculos::addElement);
        DefaultComboBoxModel<Paragem> modeloCombo = new DefaultComboBoxModel<>();
        paragens.forEach(modeloCombo::addElement);
        cmbParagens.setModel(modeloCombo);
        refrescarParagensRota();
    }

    private void refrescarParagensRota() {
        modeloParagensRota.clear();
        Rota r = listaRotas.getSelectedValue();
        if (r != null) {
            r.getParagens().forEach(modeloParagensRota::addElement);
        }
    }

    private static JButton botao(String texto, Ikon icone) {
        JButton b = new JButton(texto);
        b.setIcon(FontIcon.of(icone, 14));
        return b;
    }

    private static FontIcon iconeDe(String tipo, Color cor) {
        FontAwesomeSolid ikon;
        switch (tipo) {
            case "Machimbombo":
                ikon = FontAwesomeSolid.BUS_ALT;
                break;
            case "Chapa":
                ikon = FontAwesomeSolid.SHUTTLE_VAN;
                break;
            case "Comboio":
                ikon = FontAwesomeSolid.TRAIN;
                break;
            default:
                ikon = FontAwesomeSolid.BUS;
        }
        return FontIcon.of(ikon, 14, cor);
    }

    private static JLabel celula(JList<?> lista, String texto, Icon icone, boolean seleccionado) {
        JLabel lab = new JLabel(texto);
        lab.setIcon(icone);
        lab.setOpaque(true);
        lab.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        if (seleccionado) {
            lab.setBackground(lista.getSelectionBackground());
            lab.setForeground(lista.getSelectionForeground());
        } else {
            lab.setBackground(lista.getBackground());
            lab.setForeground(lista.getForeground());
        }
        return lab;
    }

    private void erro(SQLException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Erro na base de dados",
                JOptionPane.ERROR_MESSAGE);
    }

    private void info(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Informação",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
