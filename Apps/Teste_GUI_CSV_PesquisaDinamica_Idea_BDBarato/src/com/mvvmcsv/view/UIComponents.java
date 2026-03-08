package com.mvvmcsv.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Componentes customizados com Graphics2D — botões e campos arredondados.
 */
public class UIComponents {

    // ── Paleta ─────────────────────────────────────────────────────────────
    public static final Color BG        = new Color(18,  18,  28);
    public static final Color SURFACE   = new Color(28,  28,  42);
    public static final Color CARD      = new Color(36,  36,  54);
    public static final Color ACCENT    = new Color(99, 179, 237);
    public static final Color ACCENT2   = new Color(154, 117, 234);
    public static final Color SUCCESS   = new Color(72,  199, 142);
    public static final Color DANGER    = new Color(240,  82,  82);
    public static final Color TEXT      = new Color(226, 232, 240);
    public static final Color TEXT_MUTED= new Color(113, 128, 150);
    public static final Color BORDER    = new Color(55,  55,  80);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD,  20);
    public static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD,  12);
    public static final Font FONT_BODY  = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    // ── Botão arredondado com G2D ──────────────────────────────────────────
    public static class RoundButton extends JButton {
        private Color base;
        private Color hover;
        private boolean isHovered = false;

        public RoundButton(String text, Color base, Color hover) {
            super(text);
            this.base  = base;
            this.hover = hover;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(FONT_LABEL);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(140, 38));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { isHovered = true;  repaint(); }
                public void mouseExited (MouseEvent e) { isHovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color c = isHovered ? hover : base;
            // sombra suave
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-2, getHeight()-2, 12, 12));
            // fundo
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight()-2, 12, 12));

            // texto centrado
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth()  - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() - 1;
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    // ── Campo de texto customizado ─────────────────────────────────────────
    public static class RoundField extends JTextField {
        private final String placeholder;

        public RoundField(String placeholder, int cols) {
            super(cols);
            this.placeholder = placeholder;
            setOpaque(false);
            setForeground(TEXT);
            setCaretColor(ACCENT);
            setFont(FONT_BODY);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            setBackground(CARD);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // fundo
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

            // borda
            boolean focused = isFocusOwner();
            g2.setColor(focused ? ACCENT : BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 10, 10));

            super.paintComponent(g);

            // placeholder
            if (getText().isEmpty() && !isFocusOwner()) {
                g2.setColor(TEXT_MUTED);
                g2.setFont(FONT_BODY);
                g2.drawString(placeholder, 12, getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 1);
            }
            g2.dispose();
        }
    }

    // ── Painel com gradiente de fundo ──────────────────────────────────────
    public static class GradientPanel extends JPanel {
        public GradientPanel(LayoutManager layout) { super(layout); setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, BG, getWidth(), getHeight(), SURFACE));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ── Card panel (superfície elevada) ────────────────────────────────────
    public static class CardPanel extends JPanel {
        public CardPanel(LayoutManager layout) { super(layout); setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // sombra
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fill(new RoundRectangle2D.Float(3, 4, getWidth()-3, getHeight()-3, 16, 16));
            // fundo card
            g2.setColor(CARD);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-3, getHeight()-4, 16, 16));
            // borda subtil
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-4, getHeight()-5, 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Label com cor ──────────────────────────────────────────────────────
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setOpaque(false);
        return l;
    }
}
