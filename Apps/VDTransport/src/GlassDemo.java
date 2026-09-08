import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class GlassDemo extends JFrame {

    public GlassDemo() {
        setTitle("Glass UI Demo");
        setSize(700, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Fundo
        JPanel background = new JPanel(null);
        background.setBackground(new Color(25, 28, 40));

        // Painel Glass
        GlassPanel glass = new GlassPanel(30, 0.18f);
        glass.setBounds(120, 70, 460, 280);

        // Título
        JLabel title = new JLabel("Glass UI");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setBounds(40, 35, 300, 50);

        // Texto
        JLabel text = new JLabel(
                "<html>Componente Swing com<br>" +
                "efeito transparente / glassmorphism.</html>"
        );
        text.setForeground(new Color(220, 220, 230));
        text.setFont(new Font("SansSerif", Font.PLAIN, 16));
        text.setBounds(40, 90, 350, 60);

        // Botão
        GlassButton button = new GlassButton("Continuar");
        button.setBounds(40, 180, 150, 50);

        glass.add(title);
        glass.add(text);
        glass.add(button);

        background.add(glass);
        setContentPane(background);
    }

    // =========================================================
    // GLASS PANEL
    // =========================================================

    public static class GlassPanel extends JPanel {

        private int radius;
        private float opacity;

        public GlassPanel(int radius, float opacity) {
            this.radius = radius;
            this.opacity = opacity;

            setOpaque(false);
            setLayout(null);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            // Sombra
            g2.setColor(new Color(0, 0, 0, 80));

            g2.fillRoundRect(
                    5,
                    8,
                    getWidth() - 10,
                    getHeight() - 10,
                    radius,
                    radius
            );

            // Glass
            g2.setColor(
                    new Color(
                            255,
                            255,
                            255,
                            (int) (255 * opacity)
                    )
            );

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth() - 10,
                    getHeight() - 10,
                    radius,
                    radius
            );

            // Borda
            g2.setColor(new Color(255, 255, 255, 100));

            g2.setStroke(new BasicStroke(1.5f));

            g2.drawRoundRect(
                    0,
                    0,
                    getWidth() - 11,
                    getHeight() - 11,
                    radius,
                    radius
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // =========================================================
    // GLASS BUTTON
    // =========================================================

    public static class GlassButton extends JButton {

        private boolean hover = false;

        public GlassButton(String text) {

            super(text);

            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 14));

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);

            setCursor(
                    new Cursor(Cursor.HAND_CURSOR)
            );

            addMouseListener(new java.awt.event.MouseAdapter() {

                @Override
                public void mouseEntered(
                        java.awt.event.MouseEvent e) {

                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(
                        java.awt.event.MouseEvent e) {

                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color background;

            if (hover) {
                background = new Color(255, 255, 255, 90);
            } else {
                background = new Color(255, 255, 255, 55);
            }

            g2.setColor(background);

            g2.fill(
                    new RoundRectangle2D.Float(
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            18,
                            18
                    )
            );

            // Borda
            g2.setColor(new Color(255, 255, 255, 100));

            g2.draw(
                    new RoundRectangle2D.Float(
                            0,
                            0,
                            getWidth() - 1,
                            getHeight() - 1,
                            18,
                            18
                    )
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            new GlassDemo().setVisible(true);
        });
    }
}