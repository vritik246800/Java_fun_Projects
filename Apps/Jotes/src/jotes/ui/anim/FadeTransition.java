package jotes.ui.anim;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Window;
import java.time.Duration;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Transições de desvanecimento (fade). Todas as chamadas devem ser feitas na EDT.
 * <p>Para componentes arbitrários usa-se um painel wrapper ({@link #wrap})
 * que pinta o conteúdo com {@link AlphaComposite}; para janelas/diálogos
 * usa-se a opacidade do próprio {@link Window}.
 */
public final class FadeTransition {

    private FadeTransition() {
    }

    /** Painel wrapper que pinta o conteúdo com transparência variável. */
    public static final class FadePanel extends JPanel {

        private float alpha = 1f;

        public FadePanel(JComponent content) {
            super(new BorderLayout());
            setOpaque(false);
            if (content != null) add(content, BorderLayout.CENTER);
        }

        public float getAlpha() {
            return alpha;
        }

        /** Define a transparência (0 = invisível, 1 = opaco) e repinta. */
        public void setAlpha(float alpha) {
            this.alpha = Math.max(0f, Math.min(1f, alpha));
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            if (alpha >= 1f) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                super.paint(g2);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Envolve o conteúdo num painel com suporte de fade. */
    public static FadePanel wrap(JComponent content) {
        return new FadePanel(content);
    }

    /** Fade de entrada: o componente fica visível com alfa 0 → 1. */
    public static void fadeIn(JComponent c, Duration d) {
        fadeIn(c, d, null);
    }

    public static void fadeIn(JComponent c, Duration d, Runnable onDone) {
        fade(c, d, true, onDone);
    }

    /** Fade de saída: alfa atual → 0 e o componente fica invisível no fim. */
    public static void fadeOut(JComponent c, Duration d) {
        fadeOut(c, d, null);
    }

    public static void fadeOut(JComponent c, Duration d, Runnable onDone) {
        fade(c, d, false, onDone);
    }

    private static void fade(JComponent c, Duration d, boolean in, Runnable onDone) {
        if (!(c instanceof FadePanel fp)) {
            // Sem wrapper não há como pintar com alfa: aplica já o estado final.
            c.setVisible(in);
            if (onDone != null) onDone.run();
            return;
        }
        float from = in ? 0f : fp.getAlpha();
        float to = in ? 1f : 0f;
        if (in) {
            fp.setAlpha(0f);
            c.setVisible(true);
        }
        Animator.animate(d, Easing.EASE_OUT, v -> fp.setAlpha(from + (to - from) * (float) v), () -> {
            if (!in) {
                c.setVisible(false);
                fp.setAlpha(1f); // repõe opaco para a próxima exibição
            }
            if (onDone != null) onDone.run();
        });
    }

    /** Fade de entrada de uma janela/diálogo via opacidade do {@link Window}. */
    public static void fadeIn(Window w, Duration d) {
        fadeIn(w, d, null);
    }

    public static void fadeIn(Window w, Duration d, Runnable onDone) {
        fadeWindow(w, d, true, onDone);
    }

    /** Fade de saída de uma janela/diálogo; no fim a janela fica escondida. */
    public static void fadeOut(Window w, Duration d) {
        fadeOut(w, d, null);
    }

    public static void fadeOut(Window w, Duration d, Runnable onDone) {
        fadeWindow(w, d, false, onDone);
    }

    private static void fadeWindow(Window w, Duration d, boolean in, Runnable onDone) {
        if (!translucencySupported()) {
            w.setVisible(in);
            if (onDone != null) onDone.run();
            return;
        }
        try {
            float from = in ? 0f : w.getOpacity();
            float to = in ? 1f : 0f;
            if (in) {
                w.setOpacity(0f);
                if (!w.isVisible()) w.setVisible(true);
            }
            Animator.animate(d, Easing.EASE_IN_OUT, v -> w.setOpacity(from + (to - from) * (float) v), () -> {
                if (!in) {
                    w.setVisible(false);
                    w.setOpacity(1f); // repõe opaco para a próxima exibição
                }
                if (onDone != null) onDone.run();
            });
        } catch (UnsupportedOperationException | IllegalComponentStateException e) {
            // Janela decorada ou plataforma sem translucidez: estado final imediato.
            w.setVisible(in);
            if (onDone != null) onDone.run();
        }
    }

    private static boolean translucencySupported() {
        if (GraphicsEnvironment.isHeadless()) return false;
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
    }
}
