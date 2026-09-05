package jotes.ui;

import jotes.model.Note;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Colors;
import jotes.ui.anim.Easing;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barra de separadores das notas abertas. Cada separador mostra o título e uma
 * cruz para fechar; hover e ativação são animados. A barra esconde-se sozinha
 * quando não há nada aberto.
 */
public class NoteTabs extends JPanel {

    private static final Duration ANIM = Duration.ofMillis(140);
    private static final int HEIGHT = 34;
    private static final int MAX_WIDTH = 190;

    private final JPanel strip = new JPanel();
    private final List<Tab> tabs = new ArrayList<>();
    private Tab active;

    private Consumer<Note> onSelect = n -> {};
    private Consumer<Note> onClose = n -> {};

    public NoteTabs() {
        super(new BorderLayout());
        setBackground(Theme.LIST_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR));

        strip.setLayout(new BoxLayout(strip, BoxLayout.X_AXIS));
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        JScrollPane scroll = new JScrollPane(strip,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        Theme.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(400, HEIGHT));
        add(scroll, BorderLayout.CENTER);

        setVisible(false);
    }

    public void setOnSelect(Consumer<Note> listener) { this.onSelect = listener == null ? n -> {} : listener; }
    public void setOnClose(Consumer<Note> listener) { this.onClose = listener == null ? n -> {} : listener; }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, HEIGHT);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (strip == null) return; // chamado pelo construtor do JPanel
        setBackground(Theme.LIST_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR));
        strip.repaint();
    }

    /** Abre a nota num separador (ou ativa o existente) e notifica a seleção. */
    public void open(Note note) {
        Tab existing = find(note.getId());
        if (existing != null) {
            existing.note = note;
            activate(existing, true);
            return;
        }
        Tab tab = new Tab(note);
        tabs.add(tab);
        strip.add(tab);
        setVisible(true);
        activate(tab, true);
        revalidate();
        repaint();
    }

    /** Ativa o separador da nota sem disparar o callback (sincroniza com a lista). */
    public void setActive(long noteId) {
        Tab tab = find(noteId);
        if (tab != null) activate(tab, false);
    }

    /** Atualiza o título mostrado depois de a nota ser guardada ou renomeada. */
    public void updateTitle(Note note) {
        Tab tab = find(note.getId());
        if (tab == null) return;
        tab.note = note;
        tab.revalidate();
        tab.repaint();
    }

    /** Fecha o separador da nota, se estiver aberto. */
    public void close(long noteId) {
        Tab tab = find(noteId);
        if (tab == null) return;
        int index = tabs.indexOf(tab);
        tabs.remove(tab);
        strip.remove(tab);
        if (active == tab) {
            active = null;
            Tab next = tabs.isEmpty() ? null : tabs.get(Math.min(index, tabs.size() - 1));
            if (next != null) activate(next, true);
        }
        setVisible(!tabs.isEmpty());
        revalidate();
        repaint();
    }

    /** Fecha todos os separadores. */
    public void closeAll() {
        tabs.clear();
        strip.removeAll();
        active = null;
        setVisible(false);
        revalidate();
        repaint();
    }

    /** Passa para o separador seguinte ({@code +1}) ou anterior ({@code -1}). */
    public void cycle(int delta) {
        if (tabs.size() < 2 || active == null) return;
        int i = (tabs.indexOf(active) + delta + tabs.size()) % tabs.size();
        activate(tabs.get(i), true);
    }

    private Tab find(long noteId) {
        for (Tab t : tabs) {
            if (t.note.getId() == noteId) return t;
        }
        return null;
    }

    private void activate(Tab tab, boolean notify) {
        if (active == tab) {
            if (notify) onSelect.accept(tab.note);
            return;
        }
        Tab previous = active;
        active = tab;
        if (previous != null) previous.animateActive(0f);
        tab.animateActive(1f);
        tab.scrollRectToVisible(new Rectangle(0, 0, tab.getWidth(), HEIGHT));
        if (notify) onSelect.accept(tab.note);
    }

    /** Um separador: título recortado + cruz de fechar, com hover e ativação animados. */
    private final class Tab extends JComponent {
        private Note note;
        private float hover;
        private float activeAmount;
        private boolean overClose;
        private Animator hoverAnim;
        private Animator activeAnim;

        Tab(Note note) {
            this.note = note;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { animateHover(1f); }
                @Override public void mouseExited(MouseEvent e) {
                    overClose = false;
                    animateHover(0f);
                }
                @Override public void mouseMoved(MouseEvent e) {
                    boolean over = closeBounds().contains(e.getPoint());
                    if (over != overClose) {
                        overClose = over;
                        repaint();
                    }
                }
                @Override public void mouseClicked(MouseEvent e) {
                    if (closeBounds().contains(e.getPoint())
                            || e.getButton() == MouseEvent.BUTTON2) {
                        onClose.accept(Tab.this.note);
                    } else {
                        activate(Tab.this, true);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private Rectangle closeBounds() {
            return new Rectangle(getWidth() - 24, (HEIGHT - 16) / 2, 16, 16);
        }

        private void animateHover(float target) {
            if (hoverAnim != null) hoverAnim.cancel();
            float start = hover;
            hoverAnim = Animator.animate(ANIM, Easing.EASE_OUT, v -> {
                hover = (float) (start + (target - start) * v);
                repaint();
            });
        }

        void animateActive(float target) {
            if (activeAnim != null) activeAnim.cancel();
            float start = activeAmount;
            activeAnim = Animator.animate(ANIM, Easing.EASE_OUT, v -> {
                activeAmount = (float) (start + (target - start) * v);
                repaint();
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Theme.font(Font.PLAIN, 12));
            int width = Math.min(MAX_WIDTH, fm.stringWidth(note.displayTitle()) + 56);
            return new Dimension(Math.max(110, width), HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fill = Colors.lerp(Theme.LIST_BG, Theme.CARD_HOVER, hover);
            fill = Colors.lerp(fill, Theme.BG, activeAmount);
            g2.setColor(fill);
            g2.fillRoundRect(2, 4, getWidth() - 4, HEIGHT - 4, 8, 8);

            // sublinhado de acento no separador ativo
            if (activeAmount > 0.01f) {
                g2.setColor(Colors.lerp(Theme.LIST_BG, Theme.ACCENT, activeAmount));
                g2.fillRect(6, HEIGHT - 3, getWidth() - 12, 2);
            }

            Color fg = Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(hover, activeAmount));
            g2.setFont(Theme.font(activeAmount > 0.5f ? Font.BOLD : Font.PLAIN, 12));
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int maxText = getWidth() - 40;
            String title = note.displayTitle();
            if (fm.stringWidth(title) > maxText) {
                int i = title.length();
                while (i > 0 && fm.stringWidth(title.substring(0, i) + "…") > maxText) i--;
                title = title.substring(0, i) + "…";
            }
            g2.drawString(title, 12, (HEIGHT - fm.getHeight()) / 2 + fm.getAscent());

            Rectangle close = closeBounds();
            Icons.paintScaled(g2, Icons.CLOSE, overClose ? Theme.TEXT : Theme.TEXT_DIM,
                    close.x, close.y, 14);
            g2.dispose();
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            return new Point(8, HEIGHT);
        }

        @Override
        public String getToolTipText() {
            return note.displayTitle();
        }
    }
}
