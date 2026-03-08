package com.mvvmcsv.view;

import com.mvvmcsv.model.Person;
import com.mvvmcsv.viewmodel.PersonViewModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * VIEW — UI pura. Só fala com o ViewModel.
 */
public class MainWindow extends JFrame {

    private final PersonViewModel vm = new PersonViewModel();

    // Campos de entrada
    private final UIComponents.RoundField fieldName   = new UIComponents.RoundField("Nome", 16);
    private final UIComponents.RoundField fieldAge    = new UIComponents.RoundField("Idade", 6);
    private final UIComponents.RoundField fieldEmail  = new UIComponents.RoundField("Email", 20);
    private final UIComponents.RoundField fieldSearch = new UIComponents.RoundField("🔍  Pesquisar por ID ou Nome...", 30);

    // Tabela principal
    private final String[]          COLS_MAIN  = {"ID", "Nome", "Idade", "Email"};
    private final DefaultTableModel modelMain  = new DefaultTableModel(COLS_MAIN, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tableMain = new JTable(modelMain);

    // Tabela de resultados de pesquisa
    private final String[]          COLS_SEARCH  = {"ID", "Nome", "Idade", "Email"};
    private final DefaultTableModel modelSearch  = new DefaultTableModel(COLS_SEARCH, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tableSearch = new JTable(modelSearch);

    // Painel de pesquisa (escondido quando vazio)
    private JPanel searchResultPanel;

    // Status
    private final JLabel statusLabel = UIComponents.label("Pronto.", UIComponents.FONT_SMALL, UIComponents.TEXT_MUTED);

    // ── Construtor ─────────────────────────────────────────────────────────
    public MainWindow() {
        super("MVVM · Gestor CSV");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 650);
        setMinimumSize(new Dimension(720, 540));
        setLocationRelativeTo(null);
        bindViewModel();
        buildUI();
    }

    // ── Bind ──────────────────────────────────────────────────────────────
    private void bindViewModel() {
        vm.setOnDataChanged(this::refreshMainTable);
        vm.setOnSearchResult(this::refreshSearchTable);
        vm.setOnMessage(msg -> {
            statusLabel.setText(msg);
            statusLabel.setForeground(
                msg.startsWith("Erro") ? UIComponents.DANGER : UIComponents.SUCCESS
            );
        });
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    private void buildUI() {
        UIComponents.GradientPanel root = new UIComponents.GradientPanel(new BorderLayout(0, 0));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        setContentPane(root);
        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildCenter(),    BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Cabeçalho ──────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel titleWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, UIComponents.ACCENT, 200, 0, UIComponents.ACCENT2));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(0, getHeight()-1, 200, getHeight()-1);
                g2.dispose();
            }
        };
        titleWrap.setOpaque(false);
        titleWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        titleWrap.add(UIComponents.label("Gestor de Pessoas", UIComponents.FONT_TITLE, UIComponents.TEXT), BorderLayout.WEST);

        p.add(titleWrap, BorderLayout.NORTH);
        p.add(UIComponents.label("Lê e grava em CSV · MVVM + Swing + G2D", UIComponents.FONT_SMALL, UIComponents.TEXT_MUTED), BorderLayout.CENTER);
        return p;
    }

    // ── Centro ─────────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        p.add(buildFormCard(),   BorderLayout.NORTH);
        p.add(buildSearchBar(),  BorderLayout.CENTER);
        return p;
    }

    // ── Formulário ─────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
        UIComponents.CardPanel card = new UIComponents.CardPanel(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel fields = new JPanel(new GridLayout(1, 3, 10, 0));
        fields.setOpaque(false);
        fields.add(labeledField("Nome",  fieldName));
        fields.add(labeledField("Idade", fieldAge));
        fields.add(labeledField("Email", fieldEmail));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        UIComponents.RoundButton btnAdd    = new UIComponents.RoundButton("+ Adicionar", UIComponents.ACCENT,               UIComponents.ACCENT.brighter());
        UIComponents.RoundButton btnRemove = new UIComponents.RoundButton("− Remover",   UIComponents.DANGER,               UIComponents.DANGER.brighter());
        UIComponents.RoundButton btnSave   = new UIComponents.RoundButton("💾 Gravar",   UIComponents.ACCENT2,              UIComponents.ACCENT2.brighter());
        UIComponents.RoundButton btnLoad   = new UIComponents.RoundButton("📂 Carregar", new Color(56, 161, 105), new Color(72, 199, 142));

        btnAdd.addActionListener(e -> {
            vm.addPerson(fieldName.getText(), fieldAge.getText(), fieldEmail.getText());
            fieldName.setText(""); fieldAge.setText(""); fieldEmail.setText("");
            fieldName.requestFocus();
        });
        btnRemove.addActionListener(e -> vm.removePerson(tableMain.getSelectedRow()));
        btnSave.addActionListener(e -> vm.saveToCSV());
        btnLoad.addActionListener(e -> vm.loadFromCSV());

        btns.add(btnAdd); btns.add(btnRemove); btns.add(btnSave); btns.add(btnLoad);
        card.add(fields, BorderLayout.CENTER);
        card.add(btns,   BorderLayout.EAST);
        return card;
    }

    // ── Barra de pesquisa + tabelas ────────────────────────────────────────
    private JPanel buildSearchBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        // Campo de pesquisa com DocumentListener
        UIComponents.CardPanel searchCard = new UIComponents.CardPanel(new BorderLayout(12, 0));
        searchCard.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        JLabel searchIcon = UIComponents.label("PESQUISA", UIComponents.FONT_LABEL, UIComponents.TEXT_MUTED);
        searchRow.add(searchIcon,  BorderLayout.WEST);
        searchRow.add(fieldSearch, BorderLayout.CENTER);

        // DocumentListener — dispara a cada keystroke
        fieldSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate (DocumentEvent e) { fireSearch(); }
            @Override public void removeUpdate (DocumentEvent e) { fireSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { fireSearch(); }
            private void fireSearch() {
                SwingUtilities.invokeLater(() -> vm.search(fieldSearch.getText()));
            }
        });

        searchCard.add(searchRow, BorderLayout.CENTER);

        // Painel resultado da pesquisa (inicialmente escondido)
        searchResultPanel = buildSearchResultPanel();
        searchResultPanel.setVisible(false);

        // Tabela principal em baixo
        JPanel mainTablePanel = buildTableCard(tableMain, modelMain, "Todos os registos");

        JPanel tablesArea = new JPanel(new BorderLayout(0, 10));
        tablesArea.setOpaque(false);
        tablesArea.add(searchResultPanel, BorderLayout.NORTH);
        tablesArea.add(mainTablePanel,    BorderLayout.CENTER);

        wrapper.add(searchCard, BorderLayout.NORTH);
        wrapper.add(tablesArea, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildSearchResultPanel() {
        UIComponents.CardPanel card = new UIComponents.CardPanel(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = UIComponents.label("Resultados da pesquisa", UIComponents.FONT_LABEL, UIComponents.ACCENT);
        card.add(title, BorderLayout.NORTH);

        styleTable(tableSearch, modelSearch);
        tableSearch.setPreferredScrollableViewportSize(new Dimension(0, 120));

        JScrollPane scroll = styledScroll(tableSearch);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTableCard(JTable table, DefaultTableModel model, String titleText) {
        UIComponents.CardPanel card = new UIComponents.CardPanel(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        card.add(UIComponents.label(titleText, UIComponents.FONT_LABEL, UIComponents.TEXT_MUTED), BorderLayout.NORTH);
        styleTable(table, model);
        card.add(styledScroll(table), BorderLayout.CENTER);
        return card;
    }

    // ── Estilo tabela ──────────────────────────────────────────────────────
    private void styleTable(JTable table, DefaultTableModel model) {
        table.setBackground(UIComponents.CARD);
        table.setForeground(UIComponents.TEXT);
        table.setFont(UIComponents.FONT_BODY);
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(99, 179, 237, 60));
        table.setSelectionForeground(UIComponents.TEXT);
        table.setFocusable(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(UIComponents.SURFACE);
        header.setForeground(UIComponents.TEXT_MUTED);
        header.setFont(UIComponents.FONT_LABEL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIComponents.BORDER));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setOpaque(true);
                setBackground(sel ? new Color(99, 179, 237, 50)
                                  : row % 2 == 0 ? UIComponents.CARD : new Color(42, 42, 62));
                setForeground(col == 0 ? UIComponents.ACCENT : UIComponents.TEXT); // ID em destaque
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UIComponents.BORDER);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        // Coluna ID mais estreita, em destaque
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(260);

        for (int i = 0; i < 4; i++) table.getColumnModel().getColumn(i).setCellRenderer(renderer);
    }

    private JScrollPane styledScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUI(new MainWindow.SlimScrollBarUI());
        return scroll;
    }

    // ── Status bar ─────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 4));

        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIComponents.BORDER);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(UIComponents.label("pessoas.csv  ·  MVVM + Swing + G2D", UIComponents.FONT_SMALL, UIComponents.BORDER), BorderLayout.EAST);
        p.add(bar, BorderLayout.CENTER);
        return p;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private JPanel labeledField(String labelText, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(UIComponents.label(labelText.toUpperCase(), UIComponents.FONT_SMALL, UIComponents.TEXT_MUTED), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void refreshMainTable(List<Person> people) {
        modelMain.setRowCount(0);
        for (Person p : people)
            modelMain.addRow(new Object[]{p.getId(), p.getName(), p.getAge(), p.getEmail()});
    }

    private void refreshSearchTable(List<Person> people) {
        modelSearch.setRowCount(0);
        boolean hasResults = !people.isEmpty();
        searchResultPanel.setVisible(hasResults);
        if (hasResults) {
            for (Person p : people)
                modelSearch.addRow(new Object[]{p.getId(), p.getName(), p.getAge(), p.getEmail()});
        }
        revalidate(); repaint();
    }

    // ── SlimScrollBar ──────────────────────────────────────────────────────
    static class SlimScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(99, 179, 237, 120);
            trackColor = UIComponents.CARD;
        }
        @Override protected JButton createDecreaseButton(int o) { return invisible(); }
        @Override protected JButton createIncreaseButton(int o) { return invisible(); }
        private JButton invisible() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fill(new RoundRectangle2D.Float(r.x+2, r.y+2, r.width-4, r.height-4, 8, 8));
            g2.dispose();
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor); g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}