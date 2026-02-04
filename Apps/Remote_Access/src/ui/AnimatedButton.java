package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AnimatedButton extends JButton {

    private Color normal;
    private Color hover;
    private Color pressed;

    public AnimatedButton(String text, Color base) {
        super(text);

        normal  = base;
        hover   = base.brighter();
        pressed = base.darker();

        setBackground(normal);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                animateTo(hover);
            }

            @Override public void mouseExited(MouseEvent e) {
                animateTo(normal);
            }

            @Override public void mousePressed(MouseEvent e) {
                animateTo(pressed);
            }

            @Override public void mouseReleased(MouseEvent e) {
                animateTo(hover);
            }
        });
    }

    private void animateTo(Color target) {
        Timer timer = new Timer(10, null);
        timer.addActionListener(e -> {
            Color c = getBackground();
            int r = step(c.getRed(),   target.getRed());
            int g = step(c.getGreen(), target.getGreen());
            int b = step(c.getBlue(),  target.getBlue());

            setBackground(new Color(r, g, b));

            if (r == target.getRed() &&
                g == target.getGreen() &&
                b == target.getBlue()) {
                timer.stop();
            }
        });
        timer.start();
    }

    private int step(int c, int t) {
        if (c < t) return Math.min(c + 10, t);
        if (c > t) return Math.max(c - 10, t);
        return c;
    }
}
