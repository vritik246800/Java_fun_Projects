package jotes.ui.anim;

import java.awt.Container;
import java.awt.Point;
import java.time.Duration;
import javax.swing.JComponent;

/**
 * Desliza um componente para dentro/fora dos limites do pai, animando a
 * sua localização (útil para painéis laterais e troca de notas).
 * Todas as chamadas devem ser feitas na EDT.
 * <p>Enquanto a animação corre, o pai tem de respeitar a localização do
 * componente (layout nulo ou equivalente); no fim a posição original é
 * reposta, pelo que um layout normal volta a assumir o controlo.
 */
public final class SlideTransition {

    /** Lado por onde o componente entra (slide in) ou para onde sai (slide out). */
    public enum Direction {
        LEFT, RIGHT, UP, DOWN
    }

    private SlideTransition() {
    }

    /** O componente entra pelo lado indicado até à sua posição atual. */
    public static void slideIn(JComponent c, Direction dir, Duration d) {
        slideIn(c, dir, d, null);
    }

    public static void slideIn(JComponent c, Direction dir, Duration d, Runnable onDone) {
        slide(c, dir, d, true, onDone);
    }

    /** O componente sai pelo lado indicado e fica invisível no fim. */
    public static void slideOut(JComponent c, Direction dir, Duration d) {
        slideOut(c, dir, d, null);
    }

    public static void slideOut(JComponent c, Direction dir, Duration d, Runnable onDone) {
        slide(c, dir, d, false, onDone);
    }

    private static void slide(JComponent c, Direction dir, Duration d, boolean in, Runnable onDone) {
        Container parent = c.getParent();
        if (parent == null || c.getWidth() <= 0 || c.getHeight() <= 0) {
            // Sem contexto para animar: aplica já o estado final.
            c.setVisible(in);
            if (onDone != null) onDone.run();
            return;
        }
        Point end = c.getLocation();
        Point off = switch (dir) {
            case LEFT -> new Point(-c.getWidth(), end.y);
            case RIGHT -> new Point(parent.getWidth(), end.y);
            case UP -> new Point(end.x, -c.getHeight());
            case DOWN -> new Point(end.x, parent.getHeight());
        };
        final Point start = in ? off : end;
        final Point target = in ? end : off;
        if (in) {
            c.setLocation(start);
            c.setVisible(true);
        }
        Animator.animate(d, Easing.EASE_OUT, v -> {
            int x = (int) Math.round(start.x + (target.x - start.x) * v);
            int y = (int) Math.round(start.y + (target.y - start.y) * v);
            c.setLocation(x, y);
        }, () -> {
            c.setLocation(end);
            if (!in) c.setVisible(false);
            if (onDone != null) onDone.run();
        });
    }
}
