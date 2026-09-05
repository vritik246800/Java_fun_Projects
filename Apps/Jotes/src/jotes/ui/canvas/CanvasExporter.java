package jotes.ui.canvas;

import jotes.model.CanvasEdge;
import jotes.model.CanvasNode;
import jotes.ui.Theme;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exporta o quadro do canvas para imagem.
 * <p>O PNG é a própria pintura do {@link CanvasPanel} num contexto fora do ecrã
 * (mesmo aspeto do que está à frente do utilizador, sem grelha nem seleção).
 * O SVG é escrito a partir do modelo — cartões como {@code <rect>} arredondados,
 * ligações como {@code <path>} de Bézier —, o que dá um ficheiro vetorial limpo
 * e editável, sem precisar de nenhuma biblioteca de escrita de SVG.</p>
 */
public final class CanvasExporter {

    /** Margem à volta do conteúdo, em unidades do canvas. */
    private static final int MARGIN = 40;
    /** Fator de amostragem do PNG (2× para não sair esborratado em ecrãs HiDPI). */
    private static final int PNG_SCALE = 2;

    private CanvasExporter() {}

    /** Escreve o quadro como PNG. Devolve {@code false} se o quadro estiver vazio. */
    public static boolean exportPng(CanvasPanel panel, Path target) throws IOException {
        Rectangle2D.Double bounds = panel.contentBounds();
        if (bounds == null) return false;

        int width = (int) Math.ceil((bounds.width + MARGIN * 2) * PNG_SCALE);
        int height = (int) Math.ceil((bounds.height + MARGIN * 2) * PNG_SCALE);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();
        try {
            g2.setColor(Theme.BG);
            g2.fillRect(0, 0, width, height);
            g2.scale(PNG_SCALE, PNG_SCALE);
            g2.translate(-bounds.x + MARGIN, -bounds.y + MARGIN);
            panel.paintForExport(g2);
        } finally {
            g2.dispose();
        }
        Files.createDirectories(target.toAbsolutePath().getParent());
        ImageIO.write(image, "png", target.toFile());
        return true;
    }

    /** Escreve o quadro como SVG. Devolve {@code false} se o quadro estiver vazio. */
    public static boolean exportSvg(CanvasPanel panel, Path target) throws IOException {
        Rectangle2D.Double bounds = panel.contentBounds();
        if (bounds == null) return false;

        double width = bounds.width + MARGIN * 2;
        double height = bounds.height + MARGIN * 2;
        double dx = -bounds.x + MARGIN;
        double dy = -bounds.y + MARGIN;

        StringBuilder svg = new StringBuilder(4096);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(round(width)).append("\" height=\"").append(round(height))
                .append("\" viewBox=\"0 0 ").append(round(width)).append(' ').append(round(height))
                .append("\">\n");
        svg.append("  <rect width=\"100%\" height=\"100%\" fill=\"").append(hex(Theme.BG)).append("\"/>\n");
        svg.append("  <g font-family=\"sans-serif\" font-size=\"13\">\n");

        List<CanvasNode> nodes = panel.nodesSnapshot();

        // grupos primeiro (ficam por baixo), depois ligações, depois cartões
        for (CanvasNode n : nodes) {
            if (n.isGroup()) appendGroup(svg, panel, n, dx, dy);
        }
        for (CanvasEdge e : panel.edgesSnapshot()) {
            appendEdge(svg, panel, e, dx, dy);
        }
        for (CanvasNode n : nodes) {
            if (!n.isGroup()) appendNode(svg, panel, n, dx, dy);
        }

        svg.append("  </g>\n</svg>\n");
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.writeString(target, svg.toString(), StandardCharsets.UTF_8);
        return true;
    }

    // ---------------------------------------------------------------- peças

    private static void appendGroup(StringBuilder svg, CanvasPanel panel, CanvasNode n,
                                    double dx, double dy) {
        Color fill = panel.exportFillOf(n);
        svg.append("    <rect x=\"").append(round(n.getX() + dx)).append("\" y=\"").append(round(n.getY() + dy))
                .append("\" width=\"").append(round(n.getW())).append("\" height=\"").append(round(n.getH()))
                .append("\" rx=\"16\" fill=\"").append(hex(fill)).append("\" fill-opacity=\"0.25\"")
                .append(" stroke=\"").append(hex(Theme.SEPARATOR)).append("\" stroke-width=\"1.2\"/>\n");
        if (!n.getText().isEmpty()) {
            appendText(svg, n.getText(), n.getX() + dx + 10, n.getY() + dy + 22, Theme.TEXT_DIM, true);
        }
    }

    private static void appendNode(StringBuilder svg, CanvasPanel panel, CanvasNode n,
                                   double dx, double dy) {
        double x = n.getX() + dx;
        double y = n.getY() + dy;
        svg.append("    <rect x=\"").append(round(x)).append("\" y=\"").append(round(y))
                .append("\" width=\"").append(round(n.getW())).append("\" height=\"").append(round(n.getH()))
                .append("\" rx=\"14\" fill=\"").append(hex(panel.exportFillOf(n)))
                .append("\" stroke=\"").append(hex(Theme.SEPARATOR)).append("\" stroke-width=\"1\"/>\n");

        String title = panel.exportTitleOf(n);
        String body = panel.exportBodyOf(n);
        double textY = y + 22;
        double maxChars = Math.max(8, (n.getW() - 20) / 6.6);
        if (title != null) {
            textY = appendWrapped(svg, title, x + 10, textY, maxChars, Theme.TEXT, true,
                    n.getY() + n.getH() + dy);
            textY += 4;
        }
        if (body != null && !body.isEmpty()) {
            appendWrapped(svg, body, x + 10, textY, maxChars,
                    title != null ? Theme.TEXT_DIM : Theme.TEXT, false, y + n.getH());
        }
    }

    private static void appendEdge(StringBuilder svg, CanvasPanel panel, CanvasEdge e,
                                   double dx, double dy) {
        CubicCurve2D.Double c = panel.exportCurveOf(e);
        if (c == null) return;
        Color color = panel.exportColorOf(e);

        svg.append("    <path d=\"M ").append(round(c.getX1() + dx)).append(' ').append(round(c.getY1() + dy))
                .append(" C ").append(round(c.getCtrlX1() + dx)).append(' ').append(round(c.getCtrlY1() + dy))
                .append(", ").append(round(c.getCtrlX2() + dx)).append(' ').append(round(c.getCtrlY2() + dy))
                .append(", ").append(round(c.getX2() + dx)).append(' ').append(round(c.getY2() + dy))
                .append("\" fill=\"none\" stroke=\"").append(hex(color)).append("\" stroke-width=\"1.6\"/>\n");

        if ("arrow".equals(e.getToEnd())) {
            appendArrow(svg, new Point2D.Double(c.getX2() + dx, c.getY2() + dy),
                    angle(c, 0.97, 1.0, dx, dy), color);
        }
        if ("arrow".equals(e.getFromEnd())) {
            appendArrow(svg, new Point2D.Double(c.getX1() + dx, c.getY1() + dy),
                    angle(c, 0.03, 0.0, dx, dy), color);
        }
        if (!e.getLabel().isEmpty()) {
            Point2D.Double mid = eval(c, 0.5);
            appendText(svg, e.getLabel(), mid.x + dx, mid.y + dy, Theme.TEXT, false);
        }
    }

    private static void appendArrow(StringBuilder svg, Point2D.Double tip, double angle, Color color) {
        double s = 9;
        double x1 = tip.x - s * Math.cos(angle - 0.45);
        double y1 = tip.y - s * Math.sin(angle - 0.45);
        double x2 = tip.x - s * Math.cos(angle + 0.45);
        double y2 = tip.y - s * Math.sin(angle + 0.45);
        svg.append("    <polygon points=\"").append(round(tip.x)).append(',').append(round(tip.y))
                .append(' ').append(round(x1)).append(',').append(round(y1))
                .append(' ').append(round(x2)).append(',').append(round(y2))
                .append("\" fill=\"").append(hex(color)).append("\"/>\n");
    }

    /** Ângulo da curva no ponto {@code at}, medido a partir de {@code from}. */
    private static double angle(CubicCurve2D.Double c, double from, double at, double dx, double dy) {
        Point2D.Double a = eval(c, from);
        Point2D.Double b = eval(c, at);
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    private static Point2D.Double eval(CubicCurve2D.Double c, double t) {
        double u = 1 - t;
        double x = u * u * u * c.getX1() + 3 * u * u * t * c.getCtrlX1()
                + 3 * u * t * t * c.getCtrlX2() + t * t * t * c.getX2();
        double y = u * u * u * c.getY1() + 3 * u * u * t * c.getCtrlY1()
                + 3 * u * t * t * c.getCtrlY2() + t * t * t * c.getY2();
        return new Point2D.Double(x, y);
    }

    /** Escreve texto quebrado em linhas, parando quando sai do cartão. */
    private static double appendWrapped(StringBuilder svg, String text, double x, double y,
                                        double maxChars, Color color, boolean bold, double maxY) {
        double cursor = y;
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (!line.isEmpty() && line.length() + word.length() + 1 > maxChars) {
                    if (cursor > maxY - 4) return cursor;
                    appendText(svg, line.toString(), x, cursor, color, bold);
                    cursor += 17;
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
            if (!line.isEmpty()) {
                if (cursor > maxY - 4) return cursor;
                appendText(svg, line.toString(), x, cursor, color, bold);
                cursor += 17;
            }
        }
        return cursor;
    }

    private static void appendText(StringBuilder svg, String text, double x, double y,
                                   Color color, boolean bold) {
        svg.append("    <text x=\"").append(round(x)).append("\" y=\"").append(round(y))
                .append("\" fill=\"").append(hex(color)).append('"');
        if (bold) svg.append(" font-weight=\"bold\"");
        svg.append('>').append(escape(text)).append("</text>\n");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String round(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
