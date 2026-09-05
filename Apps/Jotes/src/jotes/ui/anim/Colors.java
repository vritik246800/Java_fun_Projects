package jotes.ui.anim;

import java.awt.Color;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Utilitários de cor para animações (hovers, seleção, etc.).
 * Todas as chamadas devem ser feitas na EDT.
 */
public final class Colors {

    private Colors() {
    }

    /** Interpolação linear ARGB entre duas cores; {@code t} é limitado a [0, 1]. */
    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                lerpChannel(a.getRed(), b.getRed(), t),
                lerpChannel(a.getGreen(), b.getGreen(), t),
                lerpChannel(a.getBlue(), b.getBlue(), t),
                lerpChannel(a.getAlpha(), b.getAlpha(), t));
    }

    private static int lerpChannel(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    /** Anima uma cor de {@code from} para {@code to}, entregando cada passo na EDT. */
    public static Animator animateColor(Duration d, Color from, Color to, Consumer<Color> onUpdate) {
        return animateColor(d, from, to, onUpdate, null);
    }

    public static Animator animateColor(Duration d, Color from, Color to, Consumer<Color> onUpdate, Runnable onDone) {
        return Animator.animate(d, Easing.EASE_IN_OUT, v -> onUpdate.accept(lerp(from, to, (float) v)), onDone);
    }
}
