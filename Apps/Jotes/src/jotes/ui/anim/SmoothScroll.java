package jotes.ui.anim;

import javax.swing.*;
import java.awt.event.MouseWheelListener;
import java.time.Duration;

/**
 * Scroll suave para {@link JScrollPane}: interpola o valor da barra vertical
 * em vez de saltar por blocos. Não faz nada quando as animações estão
 * desativadas ({@link Animations#isEnabled()}).
 */
public final class SmoothScroll {
    private static final Duration DURATION = Duration.ofMillis(160);

    private SmoothScroll() {}

    /** Substitui o comportamento da roda do rato por scroll vertical animado. */
    public static void install(JScrollPane pane) {
        for (MouseWheelListener l : pane.getMouseWheelListeners()) {
            pane.removeMouseWheelListener(l);
        }
        JScrollBar bar = pane.getVerticalScrollBar();
        int[] target = {-1};
        Animator[] current = {null};
        pane.addMouseWheelListener(e -> {
            if (!Animations.isEnabled()) {
                int unit = bar.getUnitIncrement(1) * e.getUnitsToScroll();
                bar.setValue(clamp(bar, bar.getValue() + unit));
                return;
            }
            e.consume();
            int base = target[0] >= 0 ? target[0] : bar.getValue();
            int step = Math.max(24, bar.getUnitIncrement(1)) * e.getUnitsToScroll() * 3;
            target[0] = clamp(bar, base + step);
            if (current[0] != null) current[0].cancel();
            int from = bar.getValue();
            int to = target[0];
            if (from == to) return;
            current[0] = Animator.animate(DURATION, Easing.EASE_OUT, t -> {
                bar.setValue((int) Math.round(from + (to - from) * t));
            }, () -> target[0] = -1);
        });
    }

    private static int clamp(JScrollBar bar, int v) {
        int max = bar.getMaximum() - bar.getVisibleAmount();
        return Math.max(bar.getMinimum(), Math.min(Math.max(bar.getMinimum(), max), v));
    }
}
