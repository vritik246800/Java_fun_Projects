package jotes.ui.anim;

import org.pushingpixels.trident.ease.Linear;
import org.pushingpixels.trident.ease.Spline;
import org.pushingpixels.trident.ease.TimelineEase;

/**
 * Curvas de easing standard. Implementam {@link TimelineEase}, pelo que
 * podem ser usadas diretamente com o Trident e com o {@link Animator}.
 */
public enum Easing implements TimelineEase {

    /** Progressão linear. */
    LINEAR(new Linear()),
    /** Arranque lento, fim rápido (cubic-bezier 0.42, 0, 1, 1). */
    EASE_IN(new Spline(0.42f, 0f, 1f, 1f)),
    /** Arranque rápido, fim suave (cubic-bezier 0, 0, 0.58, 1). */
    EASE_OUT(new Spline(0f, 0f, 0.58f, 1f)),
    /** Suave nos dois extremos (cubic-bezier 0.42, 0, 0.58, 1). */
    EASE_IN_OUT(new Spline(0.42f, 0f, 0.58f, 1f)),
    /** Mola com pequeno overshoot no fim (equivalente a um easeOutElastic). */
    SPRING(t -> {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        double p = 0.3;
        return (float) (Math.pow(2, -10.0 * t) * Math.sin((t - p / 4.0) * (2.0 * Math.PI) / p) + 1.0);
    });

    private final TimelineEase delegate;

    Easing(TimelineEase delegate) {
        this.delegate = delegate;
    }

    /** Mapeia o progresso linear [0, 1] para o progresso com easing. */
    @Override
    public float map(float t) {
        return delegate.map(t);
    }
}
