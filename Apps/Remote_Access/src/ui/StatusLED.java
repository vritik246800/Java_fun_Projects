package ui;

import javax.swing.*;
import java.awt.*;

public class StatusLED extends JComponent {

    private Color color = Color.RED;

    public void setOn(boolean on) {
        color = on ? new Color(0, 200, 0) : Color.RED;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(color);
        g2.fillOval(2, 2, 16, 16);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(20, 20);
    }
}
