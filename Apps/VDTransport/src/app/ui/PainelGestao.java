package app.ui;

import app.bd.BaseDados;
import app.mapa.PainelMapa;
import app.modelo.Paragem;
import app.modelo.Rota;
import app.modelo.Veiculo;
import app.rede.RedeTransportes;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.text.Normalizer;
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

    private static final Color COR_NEUTRA = new Color(0x616161);

    private final List<Paragem> paragens;
    private final List<Rota> rotas;
    private final List<Veiculo> veiculos;
    private final PainelMapa mapa;

    private final DefaultListModel<Rota> modeloRotas = new DefaultListModel<>();
    private final DefaultListModel<Paragem> modeloParagens = new DefaultListModel<>();
    private final DefaultListModel<Paragem> modeloParagensRota = new DefaultListModel<>();
    private final DefaultListModel<Veiculo> modeloVeiculos = new DefaultListModel<>();
    private final DefaultListModel<Paragem> modeloPercurso = new DefaultListModel<>();

    private final JList<Rota> listaRotas = new JList<>(modeloRotas);
    private final JList<Paragem> listaParagens = new JList<>(modeloParagens);
    private final JList<Paragem> listaParagensRota = new JList<>(modeloParagensRota);
    private final JList<Veiculo> listaVeiculos = new JList<>(modeloVeiculos);
    private final JList<Paragem> listaPercurso = new JList<>(modeloPercurso);

    private final JComboBox<Paragem> cmbParagens = new JComboBox<>();
    private final JComboBox<Paragem> cmbOrigem = new JComboBox<>();
    private final JComboBox<Paragem> cmbDestino = new JComboBox<>();
    private final JTextField txtFiltro = new JTextField();
    private final JLabel resumoPercurso = new JLabel(" ");
    private final PainelEstatisticas estatisticas;

    private RedeTransportes.Percurso percurso;

    public PainelGestao(List<Paragem> paragens, List<Rota> rotas, List<Veiculo> veiculos, PainelMapa mapa) {
        this.paragens = paragens;
        this.rotas = rotas;
        this.veiculos = veiculos;
        this.mapa = mapa;
        this.estatisticas = new PainelEstatisticas(paragens, rotas, veiculos);

        configurarListas();
        // separadores só com ícone: com o nome por extenso os cinco não cabem na largura do painel
        separador("Rotas", FontAwesomeSolid.ROUTE, criarTabRotas());
        separador("Paragens", FontAwesomeSolid.MAP_MARKER_ALT, criarTabParagens());
        separador("Veículos", FontAwesomeSolid.BUS, criarTabVeiculos());
        separador("Percurso mais curto", FontAwesomeSolid.DIRECTIONS, criarTabPercurso());
        separador("Estatísticas", FontAwesomeSolid.CHART_BAR, estatisticas);
        addChangeListener(e -> refrescar());
        refrescar();
    }

    /** Acrescenta um separador identificado só pelo ícone, com o nome em tooltip. */
    private void separador(String nome, Ikon icone, Component conteudo) {
        addTab(null, FontIcon.of(icone, 17), conteudo, nome);
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
                celula(lista, p.getNome(), FontIcon.of(FontAwesomeSolid.MAP_MARKER_ALT, 14, COR_NEUTRA), sel));
        listaParagensRota.setCellRenderer(listaParagens.getCellRenderer());

        listaVeiculos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaVeiculos.setCellRenderer((lista, v, i, sel, foco) ->
                celula(lista, v.toString(), iconeDe(v.getTipo(),
                        v.getRota() != null ? v.getRota().getCor() : COR_NEUTRA), sel));

        // no percurso, cada paragem leva a cor da rota do troço por onde se lá chega
        listaPercurso.setCellRenderer((lista, p, i, sel, foco) ->
                celula(lista, p.getNome(),
                        FontIcon.of(FontAwesomeSolid.MAP_MARKER_ALT, 14, corDoTroco(i)), sel));
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
        txtFiltro.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Procurar paragem…");
        txtFiltro.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        txtFiltro.addCaretListener(e -> refrescarParagens());

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
        painel.add(txtFiltro, BorderLayout.NORTH);
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

    private Component criarTabPercurso() {
        JButton calcular = botao("Calcular", FontAwesomeSolid.SEARCH_LOCATION);
        calcular.addActionListener(e -> calcularPercurso());
        JButton limpar = botao("Limpar", FontAwesomeSolid.ERASER);
        limpar.addActionListener(e -> limparPercurso());
        JPanel accoes = new JPanel(new GridLayout(1, 2, 4, 0));
        accoes.add(calcular);
        accoes.add(limpar);

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Origem"));
        form.add(cmbOrigem);
        form.add(new JLabel("Destino"));
        form.add(cmbDestino);
        form.add(accoes);

        resumoPercurso.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.NORTH);
        topo.add(resumoPercurso, BorderLayout.SOUTH);

        JPanel painel = new JPanel(new BorderLayout(4, 4));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(listaPercurso), BorderLayout.CENTER);
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
            alterouRede();
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
            alterouRede();
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
            alterouRede();
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
            alterouRede();
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
            alterouRede();
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
            alterouRede();
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
            alterouRede();
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
            alterouRede();
        } catch (SQLException ex) {
            erro(ex);
        }
    }

    // ---------------- percurso (Dijkstra sobre a rede) ----------------

    private void calcularPercurso() {
        Paragem origem = (Paragem) cmbOrigem.getSelectedItem();
        Paragem destino = (Paragem) cmbDestino.getSelectedItem();
        if (origem == null || destino == null) {
            info("Escolha a paragem de origem e a de destino.");
            return;
        }
        if (origem.equals(destino)) {
            info("A origem e o destino são a mesma paragem.");
            return;
        }
        percurso = new RedeTransportes(rotas).caminhoMaisCurto(origem, destino);
        modeloPercurso.clear();
        if (percurso == null) {
            mapa.setPercurso(null);
            resumoPercurso.setText("<html><b>Sem ligação</b> entre estas paragens pelas rotas actuais.</html>");
            return;
        }
        percurso.paragens().forEach(modeloPercurso::addElement);
        mapa.setPercurso(percurso.paragens());
        mapa.enquadrar(percurso.paragens());
        resumoPercurso.setText(String.format(
                "<html><b>%.0f km</b> · %d paragens · %d transbordo(s)<br>≈ %d min a %.0f km/h</html>",
                percurso.km(), percurso.paragens().size(), percurso.transbordos(),
                percurso.minutos(velocidadeMedia()), velocidadeMedia()));
    }

    private void limparPercurso() {
        percurso = null;
        modeloPercurso.clear();
        resumoPercurso.setText(" ");
        mapa.setPercurso(null);
    }

    /** Velocidade média da frota, usada para estimar a duração do percurso. */
    private double velocidadeMedia() {
        if (veiculos.isEmpty()) {
            return 60;
        }
        double total = 0;
        for (Veiculo v : veiculos) {
            total += v.getVelocidadeKmh();
        }
        return total / veiculos.size();
    }

    /** Cor da rota por onde se chega à paragem de índice indicado no percurso. */
    private Color corDoTroco(int indice) {
        if (percurso == null || percurso.rotasPorTroco().isEmpty()) {
            return COR_NEUTRA;
        }
        int troco = Math.min(Math.max(indice - 1, 0), percurso.rotasPorTroco().size() - 1);
        Rota r = percurso.rotaDoTroco(troco);
        return r != null ? r.getCor() : COR_NEUTRA;
    }

    // ---------------- utilidades ----------------

    /** Chamar depois de qualquer alteração à rede: o percurso calculado deixa de ser válido. */
    private void alterouRede() {
        limparPercurso();
        refrescar();
        mapa.repaint();
    }

    private void refrescar() {
        int seleccionada = listaRotas.getSelectedIndex();
        modeloRotas.clear();
        rotas.forEach(modeloRotas::addElement);
        if (seleccionada >= 0 && seleccionada < modeloRotas.getSize()) {
            listaRotas.setSelectedIndex(seleccionada);
        }
        refrescarParagens();
        modeloVeiculos.clear();
        veiculos.forEach(modeloVeiculos::addElement);
        recarregar(cmbParagens, paragens);
        recarregar(cmbOrigem, paragens);
        recarregar(cmbDestino, paragens);
        refrescarParagensRota();
        estatisticas.refrescar();
    }

    /** Lista de paragens do separador respectivo, filtrada pelo texto de procura. */
    private void refrescarParagens() {
        String procura = semAcentos(txtFiltro.getText().trim());
        modeloParagens.clear();
        for (Paragem p : paragens) {
            if (procura.isEmpty() || semAcentos(p.getNome()).contains(procura)) {
                modeloParagens.addElement(p);
            }
        }
    }

    private void refrescarParagensRota() {
        modeloParagensRota.clear();
        Rota r = listaRotas.getSelectedValue();
        if (r != null) {
            r.getParagens().forEach(modeloParagensRota::addElement);
        }
    }

    /** Recarrega um combo de paragens mantendo o item escolhido, se ainda existir. */
    private static void recarregar(JComboBox<Paragem> combo, List<Paragem> paragens) {
        Paragem escolhida = (Paragem) combo.getSelectedItem();
        DefaultComboBoxModel<Paragem> modelo = new DefaultComboBoxModel<>();
        paragens.forEach(modelo::addElement);
        combo.setModel(modelo);
        if (escolhida != null && paragens.contains(escolhida)) {
            combo.setSelectedItem(escolhida);
        }
    }

    /** Minúsculas e sem acentos, para a procura ignorar "Chokwé" vs "chokwe". */
    private static String semAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
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
