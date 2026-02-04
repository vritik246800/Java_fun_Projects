package ui;

import javax.swing.*;
import java.awt.*;

public class LoadingSpinner extends JComponent {

    private int angle = 0;

    public LoadingSpinner() {
        Timer timer = new Timer(50, e -> {
            angle += 15;
            repaint();
        });
        timer.start();
        setPreferredSize(new Dimension(24, 24));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setStroke(new BasicStroke(3));
        g2.setColor(new Color(0, 230, 118));
        g2.drawArc(4, 4, 16, 16, angle, 270);
    }
}
