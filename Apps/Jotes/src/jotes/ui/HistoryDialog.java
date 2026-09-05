package jotes.ui;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import jotes.db.HistoryRepository;
import jotes.model.HistoryEntry;
import jotes.model.Note;
import jotes.ui.anim.Animations;
import jotes.ui.anim.FadeTransition;
import jotes.ui.anim.SlideTransition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Diálogo modal com o histórico de versões de uma nota.
 * À esquerda a lista de versões; à direita a pré-visualização do conteúdo da versão selecionada.
 */
public class HistoryDialog extends JDialog {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    /** Durações das animações de abertura (fade da janela e slide do conteúdo), em ms. */
    private static final int FADE_MS = 200;
    private static final int SLIDE_MS = 220;

    private final DefaultListModel<HistoryEntry> model = new DefaultListModel<>();
    private final JList<HistoryEntry> list = new JList<>(model);
    private final JTextPane preview = new JTextPane();
    private final JButton restore = new JButton("Restaurar esta versão");
    private final JToggleButton diffToggle = new JToggleButton("Ver diferenças");
    private final String currentContent;
    private Consumer<HistoryEntry> onRestore = e -> {};

    public HistoryDialog(Window owner, HistoryRepository historyRepo, Note note) {
        super(owner, "Histórico — " + note.displayTitle(), ModalityType.APPLICATION_MODAL);
        this.currentContent = note.getContent();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(owner);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.LIST_BG);
        list.setForeground(Theme.TEXT);
        list.setSelectionBackground(Theme.SELECTION);
        list.setSelectionForeground(Theme.TEXT);
        list.setFont(Theme.font(Font.PLAIN, 13));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, value, index, sel, focus);
                if (value instanceof HistoryEntry entry) {
                    String title = entry.getTitle().isBlank() ? "Nova nota" : entry.getTitle();
                    // data a cor secundária, título a cor principal
                    setText("<html><span style='color:" + hex(Theme.TEXT_DIM) + "'>"
                            + FMT.format(entry.getSavedAt()) + "</span>&nbsp;—&nbsp;"
                            + escapeHtml(title) + "</html>");
                }
                setBackground(sel ? Theme.SELECTION : Theme.LIST_BG);
                setForeground(Theme.TEXT);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return this;
            }
        });

        preview.setEditable(false);
        preview.setContentType("text/html");
        preview.setBackground(Theme.BG);
        preview.setForeground(Theme.TEXT);
        preview.setCaretColor(Theme.TEXT);
        preview.setSelectionColor(Theme.TEXT_SELECTION);
        preview.setSelectedTextColor(Theme.TEXT);
        preview.setFont(Theme.font(Font.PLAIN, 13));
        preview.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        Theme.styleButton(restore);
        restore.setEnabled(false);
        restore.addActionListener(e -> {
            HistoryEntry selected = list.getSelectedValue();
            if (selected == null) return;
            onRestore.accept(selected);
            dispose();
        });
        JButton close = new JButton("Fechar");
        Theme.styleButton(close);
        close.addActionListener(e -> dispose());

        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            restore.setEnabled(list.getSelectedValue() != null);
            renderPreview();
        });

        Theme.styleButton(diffToggle);
        diffToggle.setToolTipText("Comparar esta versão com o conteúdo atual da nota");
        diffToggle.addActionListener(e -> renderPreview());

        try {
            for (HistoryEntry entry : historyRepo.list(note.getId())) model.addElement(entry);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }

        Component left;
        if (model.isEmpty()) {
            JLabel empty = new JLabel("Sem versões anteriores", SwingConstants.CENTER);
            empty.setForeground(Theme.TEXT_DIM);
            empty.setFont(Theme.font(Font.PLAIN, 13));
            left = empty;
        } else {
            JScrollPane listScroll = new JScrollPane(list);
            Theme.styleScrollPane(listScroll);
            left = listScroll;
        }

        JScrollPane previewScroll = new JScrollPane(preview);
        Theme.styleScrollPane(previewScroll);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, previewScroll);
        Theme.styleSplitPane(split);
        split.setDividerLocation(240);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        buttons.add(diffToggle);
        buttons.add(restore);
        buttons.add(close);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(split, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        // holder opaco: pinta o fundo nas zonas descobertas enquanto o conteúdo desliza
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(Theme.BG);
        holder.add(content, BorderLayout.CENTER);
        setContentPane(holder);

        // entrada animada: fade da janela + slide do conteúdo, disparada na 1.ª abertura
        // (o diálogo é modal, por isso a animação só arranca com a janela já visível)
        if (Animations.isEnabled()) {
            try {
                setOpacity(0f); // evita um flash opaco antes do fade; falha sem translucidez
            } catch (RuntimeException ignored) {}
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                removeWindowListener(this);
                playOpenAnimation(content);
            }
        });
    }

    /** Entrada animada: fade da janela e slide curto do painel de conteúdo (de cima). */
    private void playOpenAnimation(JPanel content) {
        FadeTransition.fadeIn(this, Duration.ofMillis(FADE_MS));
        Container holder = content.getParent();
        if (holder == null) return;
        LayoutManager layout = holder.getLayout();
        // layout nulo durante o slide: o pai passa a respeitar a posição animada;
        // no fim o layout original é reposto e volta a controlar o conteúdo
        holder.setLayout(null);
        content.setBounds(0, 0, holder.getWidth(), holder.getHeight());
        SlideTransition.slideIn(content, SlideTransition.Direction.UP,
                Duration.ofMillis(SLIDE_MS), () -> {
                    holder.setLayout(layout);
                    holder.revalidate();
                });
    }

    /**
     * Mostra a versão selecionada: o texto tal como estava, ou — com "Ver diferenças"
     * ligado — um diff por linhas contra o conteúdo atual da nota.
     */
    private void renderPreview() {
        HistoryEntry selected = list.getSelectedValue();
        if (selected == null) {
            preview.setText(wrapHtml(""));
            return;
        }
        preview.setText(wrapHtml(diffToggle.isSelected()
                ? diffHtml(selected.getContent(), currentContent)
                : plainHtml(selected.getContent())));
        preview.setCaretPosition(0);
    }

    /**
     * Diff por linhas entre a versão antiga e a atual (java-diff-utils): linhas
     * removidas a vermelho com {@code −}, adicionadas a verde-acento com {@code +},
     * e as iguais em cor secundária.
     */
    static String diffHtml(String oldText, String newText) {
        List<String> oldLines = Arrays.asList(oldText.split("\n", -1));
        List<String> newLines = Arrays.asList(newText.split("\n", -1));

        // marca cada linha antiga/nova com o seu papel a partir dos deltas
        List<String[]> rows = new ArrayList<>();
        int oldCursor = 0;
        for (AbstractDelta<String> delta : DiffUtils.diff(oldLines, newLines).getDeltas()) {
            int start = delta.getSource().getPosition();
            for (int i = oldCursor; i < start; i++) rows.add(new String[]{"same", oldLines.get(i)});
            if (delta.getType() != DeltaType.INSERT) {
                for (String line : delta.getSource().getLines()) rows.add(new String[]{"del", line});
            }
            if (delta.getType() != DeltaType.DELETE) {
                for (String line : delta.getTarget().getLines()) rows.add(new String[]{"ins", line});
            }
            oldCursor = start + delta.getSource().size();
        }
        for (int i = oldCursor; i < oldLines.size(); i++) rows.add(new String[]{"same", oldLines.get(i)});

        if (rows.stream().noneMatch(r -> !r[0].equals("same"))) {
            return "<p style='color:" + hex(Theme.TEXT_DIM) + "'>Esta versão é igual à atual.</p>";
        }

        StringBuilder sb = new StringBuilder("<pre>");
        for (String[] row : rows) {
            String color = switch (row[0]) {
                case "del" -> hex(Theme.DANGER);
                case "ins" -> hex(Theme.ACCENT);
                default -> hex(Theme.TEXT_DIM);
            };
            String marker = switch (row[0]) {
                case "del" -> "− ";
                case "ins" -> "+ ";
                default -> "&nbsp;&nbsp;";
            };
            sb.append("<span style='color:").append(color).append("'>")
                    .append(marker).append(escapeHtml(row[1]).isEmpty() ? "&nbsp;" : escapeHtml(row[1]))
                    .append("</span>\n");
        }
        return sb.append("</pre>").toString();
    }

    private static String plainHtml(String text) {
        return "<pre>" + escapeHtml(text) + "</pre>";
    }

    /** Envolve o corpo com o CSS da paleta ativa. */
    private static String wrapHtml(String body) {
        return "<html><head><style>"
                + "body { background-color: " + hex(Theme.BG) + "; color: " + hex(Theme.TEXT)
                + "; font-family: sans-serif; font-size: 12px; margin: 8px; }"
                + "pre { font-family: monospace; font-size: 12px; white-space: pre-wrap; margin: 0; }"
                + "</style></head><body>" + body + "</body></html>";
    }

    /** Cor da paleta em formato hexadecimal para estilos HTML. */
    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Escapa caracteres especiais para texto HTML. */
    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Regista o callback chamado com a versão escolhida ao clicar em "Restaurar esta versão". */
    public void setOnRestore(Consumer<HistoryEntry> listener) {
        this.onRestore = listener;
    }
}
