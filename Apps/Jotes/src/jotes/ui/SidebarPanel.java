package jotes.ui;

import jotes.db.FolderRepository;
import jotes.db.TagRepository;
import jotes.model.Folder;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Colors;
import jotes.ui.anim.Easing;
import jotes.ui.anim.SmoothScroll;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Painel esquerdo: secções colapsáveis (com animação de altura) para as vistas
 * fixas — Todas, Fixadas, Arquivadas, Lixeira —, as pastas e as tags.
 * A seleção atual é publicada como {@link Selection} através de
 * {@link #setOnSelectionChanged(Consumer)}.
 */
public class SidebarPanel extends JPanel {

    /** Duração das animações de hover/seleção das linhas. */
    private static final Duration ROW_ANIM = Duration.ofMillis(140);
    /** Duração da animação de expandir/colapsar uma secção. */
    private static final Duration SECTION_ANIM = Duration.ofMillis(200);

    /**
     * Filtro ativo na lista de notas. Exatamente um critério está ativo de cada vez:
     * pasta (ou todas, com {@code folderId == null}), fixadas, arquivadas, lixeira ou tag.
     */
    public record Selection(Long folderId, boolean pinnedOnly, boolean archived,
                            boolean trash, String tag) {
        public static Selection ofAll() { return new Selection(null, false, false, false, null); }
        public static Selection ofFolder(long id) { return new Selection(id, false, false, false, null); }
        public static Selection ofPinned() { return new Selection(null, true, false, false, null); }
        public static Selection ofArchived() { return new Selection(null, false, true, false, null); }
        public static Selection ofTrash() { return new Selection(null, false, false, true, null); }
        public static Selection ofTag(String name) { return new Selection(null, false, false, false, name); }
    }

    private final FolderRepository folderRepo;
    private final TagRepository tagRepo;

    private final JPanel body = new JPanel();
    private final Section viewsSection = new Section("Notas", null);
    private final Section foldersSection;
    private final Section tagsSection = new Section("Tags", null);

    private final List<Row> rows = new ArrayList<>();
    private Row selected;
    private Selection selection = Selection.ofAll();

    private Consumer<Selection> onSelectionChanged = s -> {};
    private Runnable onFoldersChanged = () -> {};

    public SidebarPanel(FolderRepository folderRepo, TagRepository tagRepo) {
        super(new BorderLayout());
        this.folderRepo = folderRepo;
        this.tagRepo = tagRepo;
        setBackground(Theme.SIDEBAR_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        foldersSection = new Section("Pastas", this::createFolder);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(viewsSection);
        body.add(Box.createVerticalStrut(10));
        body.add(foldersSection);
        body.add(Box.createVerticalStrut(10));
        body.add(tagsSection);
        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        Theme.styleScrollPane(scroll);
        SmoothScroll.install(scroll);
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    // ---------------------------------------------------------------- API

    public void setOnSelectionChanged(Consumer<Selection> listener) {
        this.onSelectionChanged = listener == null ? s -> {} : listener;
    }

    /** Chamado quando pastas são criadas, renomeadas ou apagadas. */
    public void setOnFoldersChanged(Runnable listener) {
        this.onFoldersChanged = listener == null ? () -> {} : listener;
    }

    public Selection current() {
        return selection;
    }

    /** Reconstrói as listas de pastas e tags, preservando a seleção quando possível. */
    public void refresh() {
        Selection keep = selection;
        rows.clear();
        selected = null;

        viewsSection.clearItems();
        viewsSection.addItem(row("Todas as notas", Icons.ALL, Selection.ofAll()));
        viewsSection.addItem(row("Fixadas", Icons.PIN, Selection.ofPinned()));
        viewsSection.addItem(row("Arquivadas", Icons.ARCHIVE, Selection.ofArchived()));
        viewsSection.addItem(row("Lixeira", Icons.TRASH, Selection.ofTrash()));

        foldersSection.clearItems();
        try {
            for (Folder f : folderRepo.list()) {
                Row r = row(f.getName(), Icons.FOLDER, Selection.ofFolder(f.getId()));
                r.folder = f;
                foldersSection.addItem(r);
            }
        } catch (Exception ignored) {
            // sem pastas visíveis; a app continua utilizável
        }

        tagsSection.clearItems();
        try {
            for (String t : tagRepo.listAll()) {
                tagsSection.addItem(row("#" + t, Icons.TAG, Selection.ofTag(t)));
            }
        } catch (Exception ignored) {
            // idem para as tags
        }

        // repõe a seleção anterior; se desapareceu (pasta/tag apagada) volta a "Todas"
        Row match = null;
        for (Row r : rows) {
            if (r.value.equals(keep)) { match = r; break; }
        }
        if (match == null) match = rows.get(0);
        markSelected(match, false);
        selection = match.value;

        body.revalidate();
        body.repaint();
    }

    /** Seleciona programaticamente uma vista e notifica o listener. */
    public void select(Selection target) {
        for (Row r : rows) {
            if (r.value.equals(target)) {
                r.activate();
                return;
            }
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (body == null) return; // chamado pelo construtor do JPanel
        setBackground(Theme.SIDEBAR_BG);
        for (Row r : rows) r.refreshColors();
        viewsSection.refreshColors();
        foldersSection.refreshColors();
        tagsSection.refreshColors();
    }

    // ---------------------------------------------------------------- pastas

    private void createFolder() {
        String name = JOptionPane.showInputDialog(this, "Nome da pasta:", "Nova pasta",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        try {
            folderRepo.create(name.trim());
            refresh();
            onFoldersChanged.run();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void renameFolder(Folder f) {
        String name = JOptionPane.showInputDialog(this, "Novo nome:", f.getName());
        if (name == null || name.isBlank()) return;
        try {
            folderRepo.rename(f.getId(), name.trim());
            refresh();
            onFoldersChanged.run();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void deleteFolder(Folder f) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Apagar a pasta \"" + f.getName() + "\"? As notas ficam sem pasta.",
                "Apagar pasta", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            folderRepo.delete(f.getId());
            if (selection.folderId() != null && selection.folderId() == f.getId()) {
                selection = Selection.ofAll();
            }
            refresh();
            onFoldersChanged.run();
            onSelectionChanged.accept(selection);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void error(Exception ex) {
        jotes.util.Log.warn(SidebarPanel.class, "Operação sobre pastas falhou", ex);
        Toast.error(this, ex.getMessage() == null ? "Operação falhou" : ex.getMessage());
    }

    // ---------------------------------------------------------------- linhas

    private Row row(String label, String icon, Selection value) {
        Row r = new Row(label, icon, value);
        rows.add(r);
        return r;
    }

    private void markSelected(Row r, boolean animate) {
        if (selected == r) return;
        Row previous = selected;
        selected = r;
        if (previous != null) previous.setSelected(false, animate);
        r.setSelected(true, animate);
    }

    /** Linha da sidebar: ícone + texto, com pill de fundo animada em hover e seleção. */
    private final class Row extends JComponent {
        private static final int HEIGHT = 30;

        private final String label;
        private final String iconName;
        private final Selection value;
        private Folder folder;

        private float hover;
        private float select;
        private Animator hoverAnim;
        private Animator selectAnim;

        Row(String label, String iconName, Selection value) {
            this.label = label;
            this.iconName = iconName;
            this.value = value;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(LEFT_ALIGNMENT);
            setToolTipText(label);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { animateHover(1f); }
                @Override public void mouseExited(MouseEvent e) { animateHover(0f); }
                @Override public void mousePressed(MouseEvent e) { handle(e); }
                @Override public void mouseReleased(MouseEvent e) { handle(e); }
                @Override public void mouseClicked(MouseEvent e) {
                    if (!e.isPopupTrigger()) activate();
                }
                private void handle(MouseEvent e) {
                    if (e.isPopupTrigger() && folder != null) folderMenu().show(Row.this, e.getX(), e.getY());
                }
            });
        }

        void activate() {
            markSelected(this, true);
            selection = value;
            onSelectionChanged.accept(value);
        }

        JPopupMenu folderMenu() {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem rename = new JMenuItem("Renomear");
            rename.addActionListener(e -> renameFolder(folder));
            JMenuItem delete = new JMenuItem("Apagar");
            delete.addActionListener(e -> deleteFolder(folder));
            menu.add(rename);
            menu.add(delete);
            return menu;
        }

        void setSelected(boolean on, boolean animate) {
            float target = on ? 1f : 0f;
            if (selectAnim != null) selectAnim.cancel();
            if (!animate) {
                select = target;
                repaint();
                return;
            }
            float start = select;
            selectAnim = Animator.animate(ROW_ANIM, Easing.EASE_OUT, v -> {
                select = (float) (start + (target - start) * v);
                repaint();
            });
        }

        private void animateHover(float target) {
            if (hoverAnim != null) hoverAnim.cancel();
            float start = hover;
            hoverAnim = Animator.animate(ROW_ANIM, Easing.EASE_OUT, v -> {
                hover = (float) (start + (target - start) * v);
                repaint();
            });
        }

        void refreshColors() {
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(160, HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // fundo: interpola sidebar → hover → seleção (a seleção manda)
            Color base = Theme.SIDEBAR_BG;
            Color fill = Colors.lerp(base, Theme.CARD_HOVER, hover);
            fill = Colors.lerp(fill, Theme.SELECTION, select);
            if (hover > 0.01f || select > 0.01f) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 10, 10);
            }

            Color fg = Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(hover, select));
            Icons.paintScaled(g2, iconName, fg, 8, (getHeight() - 16) / 2f, 16);

            g2.setFont(Theme.font(select > 0.5f ? Font.BOLD : Font.PLAIN, 13));
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int textX = 30;
            String text = clip(label, fm, getWidth() - textX - 8);
            g2.drawString(text, textX, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }

        private String clip(String s, FontMetrics fm, int max) {
            if (max <= 0 || fm.stringWidth(s) <= max) return s;
            String ellipsis = "…";
            int i = s.length();
            while (i > 0 && fm.stringWidth(s.substring(0, i) + ellipsis) > max) i--;
            return s.substring(0, i) + ellipsis;
        }
    }

    // ---------------------------------------------------------------- secções

    /**
     * Cabeçalho clicável + corpo com altura animada. O corpo mantém sempre a sua
     * altura preferida real; o que anima é o fator {@link Body#scale} usado em
     * {@code getPreferredSize}, com revalidate a cada pulso.
     */
    private final class Section extends JPanel {
        private final String title;
        private final JPanel header;
        private final Body content = new Body();
        private boolean expanded = true;
        private Animator anim;
        private float scale = 1f;

        Section(String title, Runnable onAdd) {
            super(new BorderLayout());
            this.title = title;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);

            header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            HeaderLabel label = new HeaderLabel();
            header.add(label, BorderLayout.CENTER);
            header.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { toggle(); }
            });

            if (onAdd != null) {
                JButton add = new JButton(Icons.themed(Icons.PLUS, 14));
                Theme.styleToolbarButton(add);
                add.setMargin(new Insets(0, 4, 0, 4));
                add.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                add.setToolTipText("Nova pasta");
                add.setFocusable(false);
                add.addActionListener(e -> onAdd.run());
                header.add(add, BorderLayout.EAST);
            }

            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);

            add(header, BorderLayout.NORTH);
            add(content, BorderLayout.CENTER);
        }

        void clearItems() {
            content.removeAll();
        }

        void addItem(JComponent c) {
            content.add(c);
        }

        void refreshColors() {
            header.repaint();
            repaint();
        }

        void toggle() {
            expanded = !expanded;
            if (anim != null) anim.cancel();
            float start = scale;
            float target = expanded ? 1f : 0f;
            anim = Animator.animate(SECTION_ANIM, Easing.EASE_IN_OUT, v -> {
                scale = (float) (start + (target - start) * v);
                content.setVisible(scale > 0.001f);
                content.revalidate();
                SidebarPanel.this.body.revalidate();
                SidebarPanel.this.body.repaint();
            }, () -> content.setVisible(expanded));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        /** Corpo cuja altura preferida é multiplicada pelo fator de animação da secção. */
        private final class Body extends JPanel {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(d.width, Math.round(d.height * scale));
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        }

        /** Título da secção com a seta a rodar entre ▸ e ▾. */
        private final class HeaderLabel extends JComponent {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(120, 22);
            }

            @Override
            protected void paintComponent(Graphics g) {
                // a seta roda continuamente com a animação (0 = ▸, 1 = ▾), num contexto próprio
                Graphics2D arrow = (Graphics2D) g.create();
                arrow.rotate(Math.toRadians(90 * scale), 8, getHeight() / 2.0);
                Icons.paintScaled(arrow, Icons.CHEVRON_RIGHT, Theme.TEXT_DIM, 2, getHeight() / 2f - 6, 12);
                arrow.dispose();

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Theme.font(Font.BOLD, 11));
                g2.setColor(Theme.TEXT_DIM);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(title.toUpperCase(java.util.Locale.ROOT), 18,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }
}
