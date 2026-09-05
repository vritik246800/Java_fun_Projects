package jotes.ui.anim;

import java.time.Duration;

/**
 * Porta global das animações: quando desligada, todas as transições e
 * animações do pacote saltam diretamente para o estado final (o callback
 * de fim corre de imediato) em vez de animarem.
 */
public final class Animations {

    private static volatile boolean enabled = true;
    private static volatile double durationScale = 1.0;

    private Animations() {
    }

    /** true se as animações estão ligadas (por omissão estão). */
    public static boolean isEnabled() {
        return enabled;
    }

    /** Liga ou desliga globalmente as animações. */
    public static void setEnabled(boolean enabled) {
        Animations.enabled = enabled;
    }

    /** Escala global aplicada à duração das animações (1.0 = normal). */
    public static double getDurationScale() {
        return durationScale;
    }

    /** Define a escala global de duração (0 torna tudo instantâneo). */
    public static void setDurationScale(double scale) {
        Animations.durationScale = Math.max(0, scale);
    }

    /** Duração já escalada, em milissegundos (uso interno do pacote). */
    static long scaledMillis(Duration d) {
        if (d == null) return 0;
        return Math.round(d.toMillis() * durationScale);
    }
}
