package ui;

import java.awt.*;

public enum Theme {
    DARK(new Color(30,30,30), Color.WHITE),
    LIGHT(Color.WHITE, Color.BLACK);

    public final Color bg;
    public final Color fg;

    Theme(Color bg, Color fg) {
        this.bg = bg;
        this.fg = fg;
    }
}
