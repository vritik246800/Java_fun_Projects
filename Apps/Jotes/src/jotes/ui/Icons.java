package jotes.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Ícones vetoriais desenhados com {@link Graphics2D} numa grelha 16×16 e escalados
 * para o tamanho pedido — substituem emojis/texto na UI sem depender de ficheiros SVG
 * nem de bibliotecas externas.
 * <p>Uso: {@code new JLabel(Icons.of(Icons.FOLDER, 16, Theme.TEXT_DIM))}. A cor é fixada
 * na criação; para acompanhar trocas de tema criar o ícone de novo em {@code updateUI()}
 * ou usar {@link #themed(String, int)}, que lê a cor da paleta a cada pintura.</p>
 */
public final class Icons {

    public static final String ALL = "all";
    public static final String NOTE = "note";
    public static final String FOLDER = "folder";
    public static final String TAG = "tag";
    public static final String PIN = "pin";
    public static final String ARCHIVE = "archive";
    public static final String TRASH = "trash";
    public static final String SEARCH = "search";
    public static final String GRAPH = "graph";
    public static final String CANVAS = "canvas";
    public static final String SETTINGS = "settings";
    public static final String CHEVRON_DOWN = "chevron-down";
    public static final String CHEVRON_RIGHT = "chevron-right";
    public static final String PLUS = "plus";
    public static final String CLOSE = "close";
    public static final String LOCK = "lock";
    public static final String CLOCK = "clock";
    public static final String CHART = "chart";
    public static final String HISTORY = "history";
    public static final String EYE = "eye";
    public static final String EDIT = "edit";
    public static final String COLUMNS = "columns";
    public static final String FOCUS = "focus";
    public static final String SUN = "sun";
    public static final String MOON = "moon";
    public static final String BOLD = "bold";
    public static final String ITALIC = "italic";
    public static final String STRIKE = "strike";
    public static final String HEADING = "heading";
    public static final String LIST = "list";
    public static final String CHECKLIST = "checklist";
    public static final String QUOTE = "quote";
    public static final String CODE = "code";
    public static final String TABLE = "table";
    public static final String LINK = "link";
    public static final String IMAGE = "image";
    public static final String PAPERCLIP = "paperclip";
    public static final String UNDO = "undo";
    public static final String REDO = "redo";
    public static final String SAVE = "save";
    public static final String EXPORT = "export";
    public static final String IMPORT = "import";
    public static final String SYNC = "sync";
    public static final String BELL = "bell";
    public static final String INFO = "info";

    private Icons() {}

    /** Ícone com cor fixa. */
    public static Icon of(String name, int size, Color color) {
        return new VectorIcon(name, size, color, false);
    }

    /** Ícone que lê {@link Theme#TEXT_DIM} a cada pintura (acompanha trocas de tema). */
    public static Icon themed(String name, int size) {
        return new VectorIcon(name, size, null, false);
    }

    /** Ícone que lê {@link Theme#ACCENT} a cada pintura. */
    public static Icon accented(String name, int size) {
        return new VectorIcon(name, size, null, true);
    }

    /** {@link Icon} que delega o desenho em {@link #paint(Graphics2D, String, Color)}. */
    private static final class VectorIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;
        private final boolean accent;

        VectorIcon(String name, int size, Color color, boolean accent) {
            this.name = name;
            this.size = size;
            this.color = color;
            this.accent = accent;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 16.0, size / 16.0);
            Color paintColor = color != null ? color : (accent ? Theme.ACCENT : Theme.TEXT_DIM);
            paint(g2, name, paintColor);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return size; }

        @Override
        public int getIconHeight() { return size; }
    }

    /**
     * Desenha o ícone {@code name} com o canto superior esquerdo em ({@code x}, {@code y})
     * e o lado {@code size}, sem alterar a transformação do contexto recebido.
     */
    public static void paintScaled(Graphics2D g, String name, Color color, float x, float y, float size) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        g2.scale(size / 16.0, size / 16.0);
        paint(g2, name, color);
        g2.dispose();
    }

    /**
     * Desenha o ícone {@code name} na grelha 16×16 do contexto atual.
     * Um nome desconhecido desenha um ponto neutro (nunca lança).
     */
    public static void paint(Graphics2D g, String name, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name == null ? "" : name) {
            case ALL -> {
                for (int i = 0; i < 3; i++) {
                    g.draw(new Rectangle2D.Float(3, 3 + i * 4.2f, 10, 2.4f));
                }
            }
            case NOTE -> {
                g.draw(new RoundRectangle2D.Float(3.5f, 2, 9, 12, 2, 2));
                g.draw(new java.awt.geom.Line2D.Float(5.5f, 5.5f, 10.5f, 5.5f));
                g.draw(new java.awt.geom.Line2D.Float(5.5f, 8, 10.5f, 8));
                g.draw(new java.awt.geom.Line2D.Float(5.5f, 10.5f, 8.5f, 10.5f));
            }
            case FOLDER -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(2, 4.5f);
                p.lineTo(6.4f, 4.5f);
                p.lineTo(7.6f, 6.2f);
                p.lineTo(14, 6.2f);
                p.lineTo(14, 13);
                p.lineTo(2, 13);
                p.closePath();
                g.draw(p);
            }
            case TAG -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(2.5f, 8.5f);
                p.lineTo(8, 3);
                p.lineTo(13.5f, 3);
                p.lineTo(13.5f, 8.5f);
                p.lineTo(8, 14);
                p.closePath();
                g.draw(p);
                g.fill(new Ellipse2D.Float(10.4f, 5.1f, 2.2f, 2.2f));
            }
            case PIN -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(6, 2.5f);
                p.lineTo(11, 2.5f);
                p.lineTo(10, 7);
                p.lineTo(12.5f, 9.5f);
                p.lineTo(4.5f, 9.5f);
                p.lineTo(7, 7);
                p.closePath();
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Float(8.5f, 9.5f, 8.5f, 14));
            }
            case ARCHIVE -> {
                g.draw(new Rectangle2D.Float(2.5f, 3, 11, 3));
                g.draw(new Rectangle2D.Float(3.5f, 6, 9, 7));
                g.draw(new java.awt.geom.Line2D.Float(6.5f, 9, 9.5f, 9));
            }
            case TRASH -> {
                g.draw(new java.awt.geom.Line2D.Float(2.5f, 4.5f, 13.5f, 4.5f));
                g.draw(new Rectangle2D.Float(4, 4.5f, 8, 9));
                g.draw(new Rectangle2D.Float(6.2f, 2.5f, 3.6f, 2));
                g.draw(new java.awt.geom.Line2D.Float(6.6f, 7, 6.6f, 11));
                g.draw(new java.awt.geom.Line2D.Float(9.4f, 7, 9.4f, 11));
            }
            case SEARCH -> {
                g.draw(new Ellipse2D.Float(3, 3, 8, 8));
                g.draw(new java.awt.geom.Line2D.Float(10, 10, 13.5f, 13.5f));
            }
            case GRAPH -> {
                g.draw(new java.awt.geom.Line2D.Float(5, 5, 11, 4.5f));
                g.draw(new java.awt.geom.Line2D.Float(5, 5, 8, 12));
                g.draw(new java.awt.geom.Line2D.Float(11, 4.5f, 8, 12));
                g.fill(new Ellipse2D.Float(3, 3, 4, 4));
                g.fill(new Ellipse2D.Float(9.5f, 2.5f, 4, 4));
                g.fill(new Ellipse2D.Float(6, 10, 4, 4));
            }
            case CANVAS -> {
                g.draw(new RoundRectangle2D.Float(2, 3, 5.5f, 4.5f, 1.5f, 1.5f));
                g.draw(new RoundRectangle2D.Float(8.5f, 8.5f, 5.5f, 4.5f, 1.5f, 1.5f));
                g.draw(new java.awt.geom.Line2D.Float(7.5f, 5.5f, 11, 5.5f));
                g.draw(new java.awt.geom.Line2D.Float(11, 5.5f, 11, 8.5f));
            }
            case SETTINGS -> {
                g.draw(new Ellipse2D.Float(5.5f, 5.5f, 5, 5));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    g.draw(new java.awt.geom.Line2D.Double(
                            8 + Math.cos(a) * 5.2, 8 + Math.sin(a) * 5.2,
                            8 + Math.cos(a) * 6.8, 8 + Math.sin(a) * 6.8));
                }
            }
            case CHEVRON_DOWN -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(4.5f, 6.5f);
                p.lineTo(8, 10);
                p.lineTo(11.5f, 6.5f);
                g.draw(p);
            }
            case CHEVRON_RIGHT -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(6.5f, 4.5f);
                p.lineTo(10, 8);
                p.lineTo(6.5f, 11.5f);
                g.draw(p);
            }
            case PLUS -> {
                g.draw(new java.awt.geom.Line2D.Float(8, 3.5f, 8, 12.5f));
                g.draw(new java.awt.geom.Line2D.Float(3.5f, 8, 12.5f, 8));
            }
            case CLOSE -> {
                g.draw(new java.awt.geom.Line2D.Float(4.5f, 4.5f, 11.5f, 11.5f));
                g.draw(new java.awt.geom.Line2D.Float(11.5f, 4.5f, 4.5f, 11.5f));
            }
            case LOCK -> {
                g.draw(new RoundRectangle2D.Float(3.5f, 7, 9, 6.5f, 1.5f, 1.5f));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(5.5f, 7);
                p.lineTo(5.5f, 5);
                p.curveTo(5.5f, 2.4f, 10.5f, 2.4f, 10.5f, 5);
                p.lineTo(10.5f, 7);
                g.draw(p);
            }
            case CLOCK, HISTORY -> {
                g.draw(new Ellipse2D.Float(2.5f, 2.5f, 11, 11));
                g.draw(new java.awt.geom.Line2D.Float(8, 5, 8, 8));
                g.draw(new java.awt.geom.Line2D.Float(8, 8, 10.5f, 9.5f));
            }
            case CHART -> {
                g.draw(new java.awt.geom.Line2D.Float(2.5f, 13.5f, 13.5f, 13.5f));
                g.fill(new Rectangle2D.Float(4, 8, 2.4f, 5));
                g.fill(new Rectangle2D.Float(7.2f, 5, 2.4f, 8));
                g.fill(new Rectangle2D.Float(10.4f, 9.5f, 2.4f, 3.5f));
            }
            case EYE -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(1.5f, 8);
                p.curveTo(4.5f, 3.5f, 11.5f, 3.5f, 14.5f, 8);
                p.curveTo(11.5f, 12.5f, 4.5f, 12.5f, 1.5f, 8);
                p.closePath();
                g.draw(p);
                g.draw(new Ellipse2D.Float(6.2f, 6.2f, 3.6f, 3.6f));
            }
            case EDIT -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(3, 13);
                p.lineTo(3.5f, 10.5f);
                p.lineTo(11, 3);
                p.lineTo(13, 5);
                p.lineTo(5.5f, 12.5f);
                p.closePath();
                g.draw(p);
            }
            case COLUMNS -> {
                g.draw(new Rectangle2D.Float(2.5f, 3, 11, 10));
                g.draw(new java.awt.geom.Line2D.Float(8, 3, 8, 13));
            }
            case FOCUS -> {
                g.draw(new Ellipse2D.Float(4.5f, 4.5f, 7, 7));
                g.draw(new java.awt.geom.Line2D.Float(2, 8, 4, 8));
                g.draw(new java.awt.geom.Line2D.Float(12, 8, 14, 8));
                g.draw(new java.awt.geom.Line2D.Float(8, 2, 8, 4));
                g.draw(new java.awt.geom.Line2D.Float(8, 12, 8, 14));
            }
            case SUN -> {
                g.fill(new Ellipse2D.Float(5.5f, 5.5f, 5, 5));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    g.draw(new java.awt.geom.Line2D.Double(
                            8 + Math.cos(a) * 6, 8 + Math.sin(a) * 6,
                            8 + Math.cos(a) * 7.4, 8 + Math.sin(a) * 7.4));
                }
            }
            case MOON -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(11.5f, 10.8f);
                p.curveTo(7.5f, 11.5f, 4.5f, 8.5f, 5.2f, 4.5f);
                p.curveTo(2.2f, 6, 2, 10.5f, 5, 12.6f);
                p.curveTo(7.6f, 14.4f, 11, 13.2f, 11.5f, 10.8f);
                p.closePath();
                g.fill(p);
            }
            case BOLD -> {
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(5, 3);
                p.lineTo(9, 3);
                p.curveTo(12.2f, 3, 12.2f, 8, 9, 8);
                p.lineTo(5, 8);
                p.closePath();
                p.moveTo(5, 8);
                p.lineTo(9.6f, 8);
                p.curveTo(13, 8, 13, 13, 9.6f, 13);
                p.lineTo(5, 13);
                p.closePath();
                g.draw(p);
            }
            case ITALIC -> {
                g.draw(new java.awt.geom.Line2D.Float(6.5f, 3, 12, 3));
                g.draw(new java.awt.geom.Line2D.Float(4, 13, 9.5f, 13));
                g.draw(new java.awt.geom.Line2D.Float(9.5f, 3, 6.5f, 13));
            }
            case STRIKE -> {
                g.draw(new java.awt.geom.Line2D.Float(3, 8, 13, 8));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(11.5f, 4.5f);
                p.curveTo(10, 2.5f, 5, 3, 5.5f, 6);
                p.moveTo(4.5f, 11.5f);
                p.curveTo(6, 13.5f, 11, 13, 10.5f, 10);
                g.draw(p);
            }
            case HEADING -> {
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Float(4, 3.5f, 4, 12.5f));
                g.draw(new java.awt.geom.Line2D.Float(11, 3.5f, 11, 12.5f));
                g.draw(new java.awt.geom.Line2D.Float(4, 8, 11, 8));
            }
            case LIST -> {
                for (int i = 0; i < 3; i++) {
                    float y = 4.2f + i * 3.8f;
                    g.fill(new Ellipse2D.Float(3, y - 1, 2, 2));
                    g.draw(new java.awt.geom.Line2D.Float(7, y, 13.5f, y));
                }
            }
            case CHECKLIST -> {
                for (int i = 0; i < 2; i++) {
                    float y = 4.5f + i * 6f;
                    g.draw(new RoundRectangle2D.Float(2.5f, y - 2, 4, 4, 1, 1));
                    g.draw(new java.awt.geom.Line2D.Float(8.5f, y, 13.5f, y));
                }
                Path2D.Float check = new Path2D.Float();
                check.moveTo(3.4f, 4.4f);
                check.lineTo(4.4f, 5.4f);
                check.lineTo(6.2f, 3.2f);
                g.draw(check);
            }
            case QUOTE -> {
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Float(3.5f, 3.5f, 3.5f, 12.5f));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Float(7, 5.5f, 13.5f, 5.5f));
                g.draw(new java.awt.geom.Line2D.Float(7, 8, 13.5f, 8));
                g.draw(new java.awt.geom.Line2D.Float(7, 10.5f, 11, 10.5f));
            }
            case CODE -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(5.5f, 4.5f);
                p.lineTo(2, 8);
                p.lineTo(5.5f, 11.5f);
                p.moveTo(10.5f, 4.5f);
                p.lineTo(14, 8);
                p.lineTo(10.5f, 11.5f);
                g.draw(p);
            }
            case TABLE -> {
                g.draw(new Rectangle2D.Float(2, 3.5f, 12, 9));
                g.draw(new java.awt.geom.Line2D.Float(2, 6.5f, 14, 6.5f));
                g.draw(new java.awt.geom.Line2D.Float(2, 9.5f, 14, 9.5f));
                g.draw(new java.awt.geom.Line2D.Float(6, 3.5f, 6, 12.5f));
                g.draw(new java.awt.geom.Line2D.Float(10, 3.5f, 10, 12.5f));
            }
            case LINK -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(6.5f, 9.5f);
                p.lineTo(9.5f, 6.5f);
                g.draw(p);
                g.draw(new java.awt.geom.Arc2D.Float(2, 7, 7, 7, 45, 180, java.awt.geom.Arc2D.OPEN));
                g.draw(new java.awt.geom.Arc2D.Float(7, 2, 7, 7, 225, 180, java.awt.geom.Arc2D.OPEN));
            }
            case IMAGE -> {
                g.draw(new RoundRectangle2D.Float(2, 3, 12, 10, 1.5f, 1.5f));
                g.fill(new Ellipse2D.Float(4.5f, 5.5f, 2.4f, 2.4f));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(2.6f, 12.4f);
                p.lineTo(6.5f, 8.5f);
                p.lineTo(9, 11);
                p.lineTo(11, 9);
                p.lineTo(13.4f, 12.4f);
                p.closePath();
                g.fill(p);
            }
            case PAPERCLIP -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(11.5f, 6.5f);
                p.lineTo(6, 12);
                p.curveTo(3.6f, 14.4f, 0.6f, 11.4f, 3, 9);
                p.lineTo(9, 3);
                p.curveTo(10.8f, 1.2f, 13.4f, 3.8f, 11.6f, 5.6f);
                p.lineTo(6, 11.2f);
                p.curveTo(5.2f, 12, 4, 10.8f, 4.8f, 10);
                p.lineTo(9.8f, 5);
                g.draw(p);
            }
            case UNDO -> {
                g.draw(new java.awt.geom.Arc2D.Float(3, 4, 10, 9, 20, 220, java.awt.geom.Arc2D.OPEN));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(3, 4.5f);
                p.lineTo(3.2f, 8.2f);
                p.lineTo(6.8f, 7.2f);
                g.draw(p);
            }
            case REDO -> {
                g.draw(new java.awt.geom.Arc2D.Float(3, 4, 10, 9, -60, -220, java.awt.geom.Arc2D.OPEN));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(13, 4.5f);
                p.lineTo(12.8f, 8.2f);
                p.lineTo(9.2f, 7.2f);
                g.draw(p);
            }
            case SAVE -> {
                g.draw(new RoundRectangle2D.Float(2.5f, 2.5f, 11, 11, 1.5f, 1.5f));
                g.draw(new Rectangle2D.Float(5, 2.5f, 6, 4));
                g.draw(new Rectangle2D.Float(5, 9, 6, 4.5f));
            }
            case EXPORT -> {
                g.draw(new java.awt.geom.Line2D.Float(8, 2.5f, 8, 10));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(5, 5.5f);
                p.lineTo(8, 2.5f);
                p.lineTo(11, 5.5f);
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Float(3, 12.5f, 13, 12.5f));
            }
            case IMPORT -> {
                g.draw(new java.awt.geom.Line2D.Float(8, 2.5f, 8, 10));
                Path2D.Float p = new Path2D.Float();
                p.moveTo(5, 7);
                p.lineTo(8, 10);
                p.lineTo(11, 7);
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Float(3, 12.5f, 13, 12.5f));
            }
            case SYNC -> {
                g.draw(new java.awt.geom.Arc2D.Float(3, 3, 10, 10, 40, 200, java.awt.geom.Arc2D.OPEN));
                g.draw(new java.awt.geom.Arc2D.Float(3, 3, 10, 10, -140, 200, java.awt.geom.Arc2D.OPEN));
                Path2D.Float a = new Path2D.Float();
                a.moveTo(2.6f, 5.6f);
                a.lineTo(3.2f, 8.4f);
                a.lineTo(6, 7.4f);
                a.moveTo(13.4f, 10.4f);
                a.lineTo(12.8f, 7.6f);
                a.lineTo(10, 8.6f);
                g.draw(a);
            }
            case BELL -> {
                Path2D.Float p = new Path2D.Float();
                p.moveTo(4, 11);
                p.lineTo(4, 7.5f);
                p.curveTo(4, 3.5f, 12, 3.5f, 12, 7.5f);
                p.lineTo(12, 11);
                p.closePath();
                g.draw(p);
                g.draw(new java.awt.geom.Line2D.Float(2.8f, 11, 13.2f, 11));
                g.draw(new java.awt.geom.Arc2D.Float(6.4f, 11.4f, 3.2f, 3, 180, 180, java.awt.geom.Arc2D.OPEN));
            }
            case INFO -> {
                g.draw(new Ellipse2D.Float(2.5f, 2.5f, 11, 11));
                g.fill(new Ellipse2D.Float(7.2f, 4.6f, 1.6f, 1.6f));
                g.draw(new java.awt.geom.Line2D.Float(8, 7.5f, 8, 11.5f));
            }
            default -> g.fill(new Ellipse2D.Float(6.5f, 6.5f, 3, 3));
        }
    }
}
