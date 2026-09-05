package jotes.ui;

import jotes.ui.anim.Animations;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Easing;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Aviso flutuante no fundo da janela — substitui {@link javax.swing.JOptionPane} nas
 * ações correntes (guardado, exportado, apagado). Surge com fade + slide, mantém-se
 * alguns segundos e desaparece; pode levar uma ação ("Anular").
 * <p>Os avisos empilham-se de baixo para cima e reposicionam-se quando um deles sai.</p>
 */
public final class Toast {

    /** Tempo visível antes do fade de saída. */
    private static final int HOLD_MS = 3600;
    private static final Duration FADE = Duration.ofMillis(180);
    private static final int MARGIN = 24;
    private static final int GAP = 8;

    /** Avisos visíveis por janela, do mais antigo para o mais recente. */
    private static final List<ToastPanel> visible = new ArrayList<>();

    private Toast() {}

    /** Aviso simples. */
    public static void show(JComponent anchor, String message) {
        show(anchor, message, null, null);
    }

    /** Aviso de erro (texto na cor {@link Theme#DANGER}). */
    public static void error(JComponent anchor, String message) {
        create(anchor, message, null, null, true);
    }

    /** Aviso com ação — por exemplo "Nota apagada" + "Anular". */
    public static void show(JComponent anchor, String message, String actionLabel, Runnable action) {
        create(anchor, message, actionLabel, action, false);
    }

    private static void create(JComponent anchor, String message,
                               String actionLabel, Runnable action, boolean danger) {
        JRootPane root = anchor == null ? null : SwingUtilities.getRootPane(anchor);
        if (root == null) return;
        JLayeredPane layers = root.getLayeredPane();

        ToastPanel toast = new ToastPanel(message, actionLabel, action, danger);
        toast.setSize(toast.getPreferredSize());
        layers.add(toast, JLayeredPane.POPUP_LAYER);
        visible.add(toast);
        layout(layers);

        if (!Animations.isEnabled()) {
            toast.alpha = 1f;
            toast.repaint();
        } else {
            Animator.animate(FADE, Easing.EASE_OUT, v -> {
                toast.alpha = (float) v;
                toast.offsetY = (float) ((1 - v) * 16);
                layout(layers);
                toast.repaint();
            });
        }

        Timer hold = new Timer(HOLD_MS, e -> dismiss(layers, toast));
        hold.setRepeats(false);
        hold.start();
        toast.holdTimer = hold;
    }

    private static void dismiss(JLayeredPane layers, ToastPanel toast) {
        if (!visible.contains(toast)) return;
        if (toast.holdTimer != null) toast.holdTimer.stop();
        Runnable remove = () -> {
            visible.remove(toast);
            layers.remove(toast);
            layers.repaint();
            layout(layers);
        };
        if (!Animations.isEnabled()) {
            remove.run();
            return;
        }
        Animator.animate(FADE, Easing.EASE_IN_OUT, v -> {
            toast.alpha = (float) (1 - v);
            toast.repaint();
        }, remove);
    }

    /** Empilha os avisos a partir do fundo da janela, o mais recente por baixo. */
    private static void layout(JLayeredPane layers) {
        int y = layers.getHeight() - MARGIN;
        for (int i = visible.size() - 1; i >= 0; i--) {
            ToastPanel t = visible.get(i);
            if (t.getParent() != layers) continue;
            Dimension d = t.getPreferredSize();
            y -= d.height;
            t.setBounds((layers.getWidth() - d.width) / 2, Math.round(y + t.offsetY), d.width, d.height);
            y -= GAP;
        }
        layers.repaint();
    }

    /** Painel arredondado do aviso, pintado com alpha próprio. */
    private static final class ToastPanel extends JComponent {
        private final JLabel label;
        private final JButton actionButton;
        float alpha;
        float offsetY;
        Timer holdTimer;

        ToastPanel(String message, String actionLabel, Runnable action, boolean danger) {
            setLayout(new BorderLayout(12, 0));
            setOpaque(false);

            label = new JLabel(message);
            label.setForeground(danger ? Theme.DANGER : Theme.TEXT);
            label.setFont(Theme.font(Font.PLAIN, 13));
            add(label, BorderLayout.CENTER);

            if (actionLabel != null && action != null) {
                actionButton = new JButton(actionLabel);
                Theme.styleToolbarButton(actionButton);
                actionButton.setForeground(Theme.ACCENT);
                actionButton.setFont(Theme.font(Font.BOLD, 13));
                actionButton.setFocusable(false);
                actionButton.addActionListener(e -> {
                    action.run();
                    JLayeredPane layers = (JLayeredPane) getParent();
                    if (layers != null) dismiss(layers, this);
                });
                add(actionButton, BorderLayout.EAST);
            } else {
                actionButton = null;
            }
            setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 10, actionButton == null ? 16 : 8));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(220, d.width), Math.max(40, d.height));
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            // sombra discreta + cartão arredondado
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, Theme.RADIUS, Theme.RADIUS);
            g2.setColor(Theme.CARD_BG);
            g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 6, Theme.RADIUS, Theme.RADIUS);
            g2.setColor(Theme.SEPARATOR);
            g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 7, Theme.RADIUS, Theme.RADIUS);
            super.paint(g2);
            g2.dispose();
        }
    }

    /** Reposiciona os avisos visíveis — chamar quando a janela muda de tamanho. */
    public static void reposition(Window window) {
        if (window == null) return;
        JRootPane root = SwingUtilities.getRootPane(window);
        if (root != null) layout(root.getLayeredPane());
    }
}
