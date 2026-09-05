package jotes.ui.anim;

import java.time.Duration;
import java.util.function.DoubleConsumer;
import javax.swing.SwingUtilities;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.TridentConfig;
import org.pushingpixels.trident.callback.TimelineCallback;

/**
 * Wrapper fino sobre o Trident para animações baseadas em duração.
 * <p>Deve ser usado apenas na EDT e nunca bloqueia: os callbacks
 * ({@code onUpdate}/{@code onDone}) são sempre entregues na EDT.
 * Se as animações estiverem desligadas ({@link Animations#isEnabled()}),
 * salta diretamente para o valor final e corre {@code onDone} de imediato.
 */
public final class Animator {

    /** Intervalo de pulso do motor Trident (~60 FPS). */
    private static final int PULSE_MS = 1000 / 60;

    static {
        try {
            // O pulso do Trident só pode ser trocado antes do motor arrancar;
            // como o Animator é a única porta de entrada, isto corre sempre a tempo.
            TridentConfig.getInstance().setPulseSource(new TridentConfig.FixedRatePulseSource(PULSE_MS));
        } catch (IllegalStateException e) {
            // Motor já a correr: mantém o pulso existente.
        }
    }

    private final Timeline timeline;
    private volatile boolean cancelled;
    private volatile boolean finished;

    private Animator(Timeline timeline) {
        this.timeline = timeline;
    }

    /**
     * Corre uma animação de {@code 0.0} a {@code 1.0} (já com easing).
     *
     * @param duration duração da animação (escalada por {@link Animations})
     * @param easing   curva de easing a aplicar
     * @param onUpdate consumidor do valor interpolado, chamado na EDT a cada pulso
     * @param onDone   callback de fim, chamado na EDT (pode ser null)
     * @return handle para cancelar a animação
     */
    public static Animator animate(Duration duration, Easing easing, DoubleConsumer onUpdate, Runnable onDone) {
        long ms = Animations.scaledMillis(duration);
        if (!Animations.isEnabled() || ms <= 0) {
            Animator a = new Animator(null);
            a.finished = true;
            onUpdate.accept(1.0);
            if (onDone != null) onDone.run();
            return a;
        }
        Animator a = new Animator(new Timeline());
        a.timeline.setDuration(ms);
        a.timeline.setEase(easing);
        a.timeline.addCallback(new TimelineCallback() {
            @Override
            public void onTimelinePulse(float durationFraction, float timelinePosition) {
                runOnEdt(() -> {
                    if (!a.cancelled) onUpdate.accept(timelinePosition);
                });
            }

            @Override
            public void onTimelineStateChanged(Timeline.TimelineState oldState, Timeline.TimelineState newState,
                    float durationFraction, float timelinePosition) {
                if (newState == Timeline.TimelineState.DONE) {
                    runOnEdt(() -> {
                        if (a.cancelled) return;
                        a.finished = true;
                        onUpdate.accept(1.0);
                        if (onDone != null) onDone.run();
                    });
                }
            }
        });
        a.timeline.play();
        return a;
    }

    /** Atalho sem callback de fim. */
    public static Animator animate(Duration duration, Easing easing, DoubleConsumer onUpdate) {
        return animate(duration, easing, onUpdate, null);
    }

    /** Cancela a animação ({@code onDone} não é chamado). Seguro fora da EDT. */
    public void cancel() {
        cancelled = true;
        Timeline t = timeline;
        if (t != null) t.cancel();
    }

    /** true enquanto a animação está a correr. */
    public boolean isRunning() {
        return !finished && !cancelled;
    }

    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
