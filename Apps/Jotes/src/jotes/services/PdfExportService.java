package jotes.services;

import jotes.model.Note;
import jotes.ui.ThemeColors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Exporta notas para PDF com PDFBox, aplicando as cores da paleta ativa.
 * O markdown não é renderizado como HTML: é percorrido linha a linha e desenhado
 * com o estilo certo — títulos, listas, citações, blocos de código, imagens
 * anexadas e regras horizontais —, que é o suficiente para uma nota impressa.
 */
public class PdfExportService {

    private static final float MARGIN = 56;
    private static final float LEADING = 1.35f;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final AttachmentStorage storage;

    public PdfExportService(AttachmentStorage storage) {
        this.storage = storage;
    }

    /** Escreve uma nota como PDF em {@code target}. */
    public void export(Note note, Path target, ThemeColors palette) throws Exception {
        export(List.of(note), target, palette);
    }

    /** Escreve várias notas no mesmo PDF, uma por página (ou mais, se forem longas). */
    public void export(List<Note> notes, Path target, ThemeColors palette) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            for (Note note : notes) {
                Writer writer = new Writer(doc, palette);
                writer.newPage();
                writer.title(note.displayTitle());
                if (note.getUpdatedAt() != null) {
                    writer.meta(FMT.format(note.getUpdatedAt())
                            + (note.getTags().isEmpty() ? "" : "   ·   #" + String.join("  #", note.getTags())));
                }
                writer.gap(10);
                writer.markdown(note.getContent());
                writer.close();
            }
            Files.createDirectories(target.toAbsolutePath().getParent());
            doc.save(target.toFile());
        }
    }

    /** Cursor de escrita sobre o documento: gere páginas, fontes e quebras de linha. */
    private final class Writer implements AutoCloseable {
        private final PDDocument doc;
        private final ThemeColors palette;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final PDType1Font italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        private final PDType1Font mono = new PDType1Font(Standard14Fonts.FontName.COURIER);

        Writer(PDDocument doc, ThemeColors palette) {
            this.doc = doc;
            this.palette = palette;
        }

        void newPage() throws Exception {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            // fundo da paleta em toda a página
            stream.setNonStrokingColor(awt(palette.bg()));
            stream.addRect(0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            stream.fill();
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        float width() {
            return page.getMediaBox().getWidth() - MARGIN * 2;
        }

        void ensure(float needed) throws Exception {
            if (y - needed < MARGIN) newPage();
        }

        void gap(float amount) {
            y -= amount;
        }

        void title(String text) throws Exception {
            write(text, bold, 20, palette.text(), 0);
            gap(4);
        }

        void meta(String text) throws Exception {
            write(text, regular, 9, palette.textDim(), 0);
        }

        /**
         * Percorre o markdown linha a linha. Cobre títulos, listas (incluindo
         * tarefas), citações, blocos de código cercados, imagens e regras.
         */
        void markdown(String markdown) throws Exception {
            boolean inCode = false;
            for (String raw : (markdown == null ? "" : markdown).split("\n", -1)) {
                String line = raw.stripTrailing();

                if (line.trim().startsWith("```")) {
                    inCode = !inCode;
                    gap(4);
                    continue;
                }
                if (inCode) {
                    write(line.isEmpty() ? " " : line, mono, 9.5f, palette.text(), 8);
                    continue;
                }
                if (line.isBlank()) {
                    gap(7);
                    continue;
                }
                if (line.matches("^\\s*([-*_])\\s*\\1\\s*\\1[\\s\\-*_]*$")) {
                    rule();
                    continue;
                }

                String image = imageTarget(line);
                if (image != null) {
                    image(image);
                    continue;
                }

                int heading = 0;
                while (heading < line.length() && line.charAt(heading) == '#') heading++;
                if (heading > 0 && heading <= 6 && heading < line.length() && line.charAt(heading) == ' ') {
                    float size = switch (heading) {
                        case 1 -> 17;
                        case 2 -> 15;
                        case 3 -> 13.5f;
                        default -> 12.5f;
                    };
                    gap(6);
                    write(strip(line.substring(heading + 1).trim()), bold, size, palette.text(), 0);
                    gap(2);
                    continue;
                }
                if (line.trim().startsWith(">")) {
                    write(strip(line.trim().substring(1).trim()), italic, 11, palette.textDim(), 14);
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches("^\\d+\\.\\s.*")) {
                    String bullet = trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]") ? "☑ "
                            : trimmed.startsWith("- [ ]") ? "☐ " : "•  ";
                    String text = trimmed.replaceFirst("^(- \\[[ xX]\\]|[-*]|\\d+\\.)\\s*", "");
                    float indent = (line.length() - line.stripLeading().length()) * 3f;
                    write(bullet + strip(text), regular, 11, palette.text(), 12 + indent);
                    continue;
                }
                write(strip(line), regular, 11, palette.text(), 0);
            }
        }

        /** Caminho do anexo se a linha for só uma imagem markdown; {@code null} caso contrário. */
        private String imageTarget(String line) {
            String t = line.trim();
            if (!t.startsWith("![") || !t.endsWith(")")) return null;
            int open = t.indexOf("](");
            if (open < 0) return null;
            String target = t.substring(open + 2, t.length() - 1).trim();
            return target.startsWith("attachment:")
                    ? target.substring("attachment:".length()).replace("%20", " ") : null;
        }

        private void image(String relpath) throws Exception {
            Path file = storage.resolve(relpath);
            if (!Files.exists(file)) {
                write("[imagem em falta: " + relpath + "]", italic, 10, palette.textDim(), 0);
                return;
            }
            PDImageXObject img = PDImageXObject.createFromFile(file.toString(), doc);
            float scale = Math.min(1f, width() / img.getWidth());
            float w = img.getWidth() * scale;
            float h = img.getHeight() * scale;
            if (h > page.getMediaBox().getHeight() - MARGIN * 2) {
                // imagem mais alta que a página: encolhe para caber
                scale = (page.getMediaBox().getHeight() - MARGIN * 2) / img.getHeight();
                w = img.getWidth() * scale;
                h = img.getHeight() * scale;
            }
            ensure(h + 8);
            stream.drawImage(img, MARGIN, y - h, w, h);
            y -= h + 8;
        }

        private void rule() throws Exception {
            ensure(12);
            stream.setStrokingColor(awt(palette.separator()));
            stream.setLineWidth(0.7f);
            stream.moveTo(MARGIN, y - 4);
            stream.lineTo(MARGIN + width(), y - 4);
            stream.stroke();
            y -= 14;
        }

        /** Escreve texto com quebra automática, respeitando a margem e a indentação. */
        private void write(String text, PDType1Font font, float size, Color color, float indent)
                throws Exception {
            float lineHeight = size * LEADING;
            for (String line : wrap(text, font, size, width() - indent)) {
                ensure(lineHeight);
                stream.beginText();
                stream.setFont(font, size);
                stream.setNonStrokingColor(awt(color));
                stream.newLineAtOffset(MARGIN + indent, y - size);
                stream.showText(sanitize(line));
                stream.endText();
                y -= lineHeight;
            }
        }

        private List<String> wrap(String text, PDType1Font font, float size, float max) throws Exception {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (textWidth(candidate, font, size) <= max || current.isEmpty()) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            lines.add(current.toString());
            return lines;
        }

        private float textWidth(String text, PDType1Font font, float size) throws Exception {
            return font.getStringWidth(sanitize(text)) / 1000 * size;
        }

        /** As fontes Standard 14 são WinAnsi: substitui o que não conseguem representar. */
        private String sanitize(String text) {
            StringBuilder sb = new StringBuilder(text.length());
            for (char c : text.toCharArray()) {
                if (c == '☑') sb.append("[x]");
                else if (c == '☐') sb.append("[ ]");
                else if (c == '•') sb.append('-');
                else if (c == '…') sb.append("...");
                else if (c == '\t') sb.append("    ");
                else if (c < 32) sb.append(' ');
                else if (c > 255) sb.append('?');
                else sb.append(c);
            }
            return sb.toString();
        }

        /** Remove a marcação inline (negrito, itálico, código, links) do texto. */
        private String strip(String s) {
            return s.replaceAll("!\\[(.*?)\\]\\((.+?)\\)", "$1")
                    .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                    .replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1")
                    .replaceAll("~~(.+?)~~", "$1")
                    .replaceAll("`(.+?)`", "$1")
                    .replaceAll("\\[\\[([^\\[\\]|]+?)(?:\\|([^\\[\\]]*?))?\\]\\]", "$1")
                    .replaceAll("\\[(.+?)\\]\\((.+?)\\)", "$1")
                    .replaceAll("</?u>", "");
        }

        private Color awt(Color c) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue());
        }

        @Override
        public void close() throws Exception {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
