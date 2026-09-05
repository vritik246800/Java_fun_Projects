package jotes.ui;

import jotes.db.NoteRepository;
import jotes.db.TagRepository;
import jotes.model.Note;
import jotes.ui.anim.Animations;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Easing;
import jotes.ui.anim.SmoothScroll;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Paleta de comandos e pesquisa global (Ctrl+K / Ctrl+P): num só campo procura
 * comandos registados em {@link Shortcuts}, notas (título e conteúdo) e tags,
 * ordenados por afinidade — prefixo &gt; subcadeia &gt; subsequência &gt;
 * Jaro-Winkler (Apache Commons Text).
 * <p>Os resultados de nota mostram o trecho onde a pesquisa acertou, com o termo
 * realçado, tal como pede a "busca global com destaque".</p>
 */
public class CommandPalette extends JDialog {

    /** Máximo de notas listadas de cada vez (a paleta não é um explorador). */
    private static final int MAX_NOTES = 40;
    /** Debounce da pesquisa na base de dados. */
    private static final int DEBOUNCE_MS = 140;

    private static final JaroWinklerSimilarity SIMILARITY = new JaroWinklerSimilarity();

    /** Uma linha de resultado: comando, nota ou tag. */
    private record Item(String icon, String title, String subtitle, String hint,
                        String snippet, int matchStart, int matchEnd, Runnable action, double score) {}

    private final NoteRepository noteRepo;
    private final TagRepository tagRepo;
    private final Shortcuts shortcuts;

    private final Theme.PlaceholderField field =
            new Theme.PlaceholderField("Procurar notas, comandos e tags…");
    private final DefaultListModel<Item> model = new DefaultListModel<>();
    private final JList<Item> results = new JList<>(model);
    private final Timer debounce;

    private Consumer<Note> onOpenNote = n -> {};
    private Consumer<String> onOpenTag = t -> {};

    public CommandPalette(Window owner, NoteRepository noteRepo, TagRepository tagRepo,
                          Shortcuts shortcuts) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.noteRepo = noteRepo;
        this.tagRepo = tagRepo;
        this.shortcuts = shortcuts;

        setUndecorated(true);
        setSize(640, 440);

        field.setFont(Theme.font(Font.PLAIN, 16));
        field.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        field.setBackground(Theme.CARD_BG);

        debounce = new Timer(DEBOUNCE_MS, e -> search());
        debounce.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });

        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setBackground(Theme.BG);
        results.setFixedCellHeight(46);
        results.setCellRenderer(new ItemRenderer());
        results.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1 && results.getSelectedValue() != null) accept();
            }
        });

        JScrollPane scroll = new JScrollPane(results);
        Theme.styleScrollPane(scroll);
        SmoothScroll.install(scroll);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createLineBorder(Theme.SEPARATOR));

        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(Theme.CARD_BG);
        head.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR));
        head.add(field, BorderLayout.CENTER);

        content.add(head, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        setContentPane(content);

        bindKeys();
    }

    /** Chamado com a nota escolhida na paleta. */
    public void setOnOpenNote(Consumer<Note> listener) {
        this.onOpenNote = listener == null ? n -> {} : listener;
    }

    /** Chamado com a tag escolhida na paleta. */
    public void setOnOpenTag(Consumer<String> listener) {
        this.onOpenTag = listener == null ? t -> {} : listener;
    }

    /** Abre a paleta centrada no topo da janela dona, com o campo limpo e focado. */
    public void open() {
        Window owner = getOwner();
        if (owner != null) {
            setLocation(owner.getX() + (owner.getWidth() - getWidth()) / 2,
                    owner.getY() + Math.max(40, owner.getHeight() / 6));
        }
        field.setText("");
        search();
        if (Animations.isEnabled()) {
            try {
                setOpacity(0f);
                Animator.animate(Duration.ofMillis(140), Easing.EASE_OUT, v -> {
                    try { setOpacity((float) v); } catch (RuntimeException ignored) {}
                });
            } catch (RuntimeException ignored) {
                // sem translucidez: abre sem fade
            }
        }
        SwingUtilities.invokeLater(field::requestFocusInWindow);
        setVisible(true);
    }

    // ---------------------------------------------------------------- teclado

    private void bindKeys() {
        bind(field, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar", this::dispose);
        bind(field, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "aceitar", this::accept);
        bind(field, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "desce", () -> move(1));
        bind(field, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "sobe", () -> move(-1));
        bind(field, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "descePagina", () -> move(8));
        bind(field, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "sobePagina", () -> move(-8));
    }

    private void bind(JComponent c, KeyStroke stroke, String name, Runnable action) {
        c.getInputMap(JComponent.WHEN_FOCUSED).put(stroke, name);
        c.getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void move(int delta) {
        if (model.isEmpty()) return;
        int i = Math.max(0, Math.min(model.size() - 1, results.getSelectedIndex() + delta));
        results.setSelectedIndex(i);
        results.ensureIndexIsVisible(i);
    }

    private void accept() {
        Item item = results.getSelectedValue();
        if (item == null) return;
        dispose();
        // corre depois de fechar para o foco assentar na janela principal
        SwingUtilities.invokeLater(item.action());
    }

    // ---------------------------------------------------------------- pesquisa

    private void search() {
        String q = field.getText().trim();
        List<Item> items = new ArrayList<>();

        // ">" força só comandos, "#" só tags — atalhos de sintaxe habituais
        boolean commandsOnly = q.startsWith(">");
        boolean tagsOnly = q.startsWith("#");
        String term = (commandsOnly || tagsOnly) ? q.substring(1).trim() : q;

        if (!tagsOnly) items.addAll(commandItems(term));
        if (!commandsOnly) items.addAll(tagItems(term));
        if (!commandsOnly && !tagsOnly) items.addAll(noteItems(term));

        items.sort((a, b) -> Double.compare(b.score(), a.score()));

        model.clear();
        for (Item i : items) model.addElement(i);
        if (!model.isEmpty()) results.setSelectedIndex(0);
    }

    private List<Item> commandItems(String term) {
        List<Item> out = new ArrayList<>();
        for (Shortcuts.Command c : shortcuts.commands()) {
            double score = score(c.label(), term);
            if (score <= 0) continue;
            out.add(new Item(c.icon(), c.label(), c.group(), c.strokeText(),
                    null, -1, -1, c::run, score + 0.25)); // comandos ligeiramente à frente
        }
        return out;
    }

    private List<Item> tagItems(String term) {
        List<Item> out = new ArrayList<>();
        try {
            for (String tag : tagRepo.listAll()) {
                double score = score(tag, term);
                if (score <= 0) continue;
                out.add(new Item(Icons.TAG, "#" + tag, "Tag", null, null, -1, -1,
                        () -> onOpenTag.accept(tag), score + 0.1));
            }
        } catch (Exception ignored) {
            // sem tags: a paleta continua a servir comandos e notas
        }
        return out;
    }

    private List<Item> noteItems(String term) {
        List<Item> out = new ArrayList<>();
        try {
            NoteRepository.Query q = NoteRepository.Query.all().withPage(MAX_NOTES, 0);
            List<Note> notes = term.isEmpty() ? noteRepo.list(q) : noteRepo.list(q.withText(term));
            for (Note n : notes) {
                double titleScore = score(n.displayTitle(), term);
                int[] hit = term.isEmpty() ? null : findMatch(n.getContent(), term);
                if (titleScore <= 0 && hit == null) continue;

                String snippet = null;
                int start = -1;
                int end = -1;
                if (hit != null) {
                    // trecho com ~40 caracteres de contexto de cada lado, numa só linha
                    String flat = n.getContent().replaceAll("\\s+", " ");
                    int[] flatHit = findMatch(flat, term);
                    if (flatHit != null) {
                        int from = Math.max(0, flatHit[0] - 40);
                        int to = Math.min(flat.length(), flatHit[1] + 60);
                        String prefix = from > 0 ? "… " : "";
                        snippet = prefix + flat.substring(from, to) + (to < flat.length() ? " …" : "");
                        start = prefix.length() + (flatHit[0] - from);
                        end = start + (flatHit[1] - flatHit[0]);
                    }
                }
                double score = Math.max(titleScore, hit != null ? 0.45 : 0);
                out.add(new Item(Icons.NOTE, n.displayTitle(),
                        n.getTags().isEmpty() ? "Nota" : "#" + String.join(" #", n.getTags()),
                        null, snippet, start, end, () -> onOpenNote.accept(n), score));
            }
        } catch (Exception ignored) {
            // pesquisa falhada não deve fechar a paleta
        }
        return out;
    }

    /** Índices [início, fim) da primeira ocorrência de {@code term}, ignorando maiúsculas. */
    private static int[] findMatch(String text, String term) {
        if (text == null || term.isEmpty()) return null;
        int i = text.toLowerCase(Locale.ROOT).indexOf(term.toLowerCase(Locale.ROOT));
        return i < 0 ? null : new int[]{i, i + term.length()};
    }

    /**
     * Afinidade entre 0 (não serve) e 1 (perfeita): prefixo &gt; subcadeia &gt;
     * subsequência difusa &gt; Jaro-Winkler. Termo vazio devolve uma base positiva
     * para a paleta mostrar tudo antes de se escrever.
     */
    static double score(String candidate, String term) {
        if (term == null || term.isEmpty()) return 0.3;
        String c = candidate.toLowerCase(Locale.ROOT);
        String t = term.toLowerCase(Locale.ROOT);
        if (c.equals(t)) return 1.0;
        if (c.startsWith(t)) return 0.9;
        if (c.contains(t)) return 0.75;
        if (isSubsequence(c, t)) return 0.6;
        double jw = SIMILARITY.apply(c, t);
        return jw >= 0.82 ? jw * 0.5 : 0;
    }

    /** {@code true} se as letras de {@code term} aparecem por ordem em {@code text}. */
    private static boolean isSubsequence(String text, String term) {
        int i = 0;
        for (int j = 0; j < text.length() && i < term.length(); j++) {
            if (text.charAt(j) == term.charAt(i)) i++;
        }
        return i == term.length();
    }

    // ---------------------------------------------------------------- render

    /** Linha: ícone, título, subtítulo/atalho e, nas notas, o trecho com o termo realçado. */
    private final class ItemRenderer extends JComponent implements ListCellRenderer<Item> {
        private Item item;
        private boolean selected;

        @Override
        public Component getListCellRendererComponent(JList<? extends Item> list, Item value,
                                                      int index, boolean isSelected, boolean hasFocus) {
            this.item = value;
            this.selected = isSelected;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(600, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (item == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(selected ? Theme.SELECTION : Theme.BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Icons.paintScaled(g2, item.icon(), selected ? Theme.TEXT : Theme.TEXT_DIM, 14, 15, 16);

            g2.setFont(Theme.font(Font.PLAIN, 13));
            g2.setColor(Theme.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            int textX = 42;
            g2.drawString(item.title(), textX, 20);

            // atalho do comando, alinhado à direita
            if (item.hint() != null && !item.hint().isEmpty()) {
                g2.setFont(Theme.font(Font.PLAIN, 11));
                g2.setColor(Theme.TEXT_DIM);
                FontMetrics hintFm = g2.getFontMetrics();
                g2.drawString(item.hint(), getWidth() - hintFm.stringWidth(item.hint()) - 16, 20);
            }

            g2.setFont(Theme.font(Font.PLAIN, 11));
            g2.setColor(Theme.TEXT_DIM);
            if (item.snippet() != null) {
                drawHighlighted(g2, item.snippet(), item.matchStart(), item.matchEnd(),
                        textX, 36, getWidth() - textX - 16);
            } else if (item.subtitle() != null) {
                g2.drawString(clip(item.subtitle(), g2.getFontMetrics(), getWidth() - textX - 16), textX, 36);
            }
            g2.dispose();
        }

        /** Desenha o trecho com o intervalo [start, end) sobre fundo de acento. */
        private void drawHighlighted(Graphics2D g2, String text, int start, int end,
                                     int x, int y, int maxWidth) {
            FontMetrics fm = g2.getFontMetrics();
            String shown = clip(text, fm, maxWidth);
            if (start < 0 || start >= shown.length()) {
                g2.drawString(shown, x, y);
                return;
            }
            int stop = Math.min(end, shown.length());
            String before = shown.substring(0, start);
            String match = shown.substring(start, stop);
            String after = shown.substring(stop);

            int bx = x + fm.stringWidth(before);
            g2.setColor(Theme.ACCENT_BG);
            g2.fillRoundRect(bx - 2, y - fm.getAscent(), fm.stringWidth(match) + 4, fm.getHeight(), 4, 4);

            g2.setColor(Theme.TEXT_DIM);
            g2.drawString(before, x, y);
            g2.setColor(Theme.TEXT);
            g2.drawString(match, bx, y);
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString(after, bx + fm.stringWidth(match), y);
        }

        private String clip(String s, FontMetrics fm, int max) {
            if (max <= 0 || fm.stringWidth(s) <= max) return s;
            int i = s.length();
            while (i > 0 && fm.stringWidth(s.substring(0, i) + "…") > max) i--;
            return s.substring(0, i) + "…";
        }
    }

    /** Cor com alpha, usada nos realces. */
    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
