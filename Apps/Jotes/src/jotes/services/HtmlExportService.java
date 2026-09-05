package jotes.services;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.misc.Extension;
import jotes.db.FolderRepository;
import jotes.db.NoteRepository;
import jotes.model.Folder;
import jotes.model.Note;
import jotes.ui.ThemeColors;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exporta notas para um site HTML estático navegável: uma página por nota, um
 * índice com as pastas e uma folha de estilo derivada da paleta escolhida.
 * Os {@code [[wikilinks]]} passam a hiperligações entre páginas e os anexos são
 * copiados para {@code attachments/}.
 */
public class HtmlExportService {

    private static final List<Extension> EXTENSIONS =
            List.of(TablesExtension.create(), StrikethroughExtension.create());

    private static final Pattern WIKILINK =
            Pattern.compile("\\[\\[([^\\[\\]|]+?)(?:\\|([^\\[\\]]*?))?\\]\\]");
    private static final Pattern ATTACH_ATTR =
            Pattern.compile("(src|href)=\"attachment:([^\"]+)\"");

    private final NoteRepository noteRepo;
    private final FolderRepository folderRepo;
    private final AttachmentStorage storage;

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    public HtmlExportService(NoteRepository noteRepo, FolderRepository folderRepo,
                             AttachmentStorage storage) {
        this.noteRepo = noteRepo;
        this.folderRepo = folderRepo;
        this.storage = storage;
    }

    /**
     * Escreve o site em {@code target}. {@code folderId} a {@code null} exporta tudo.
     * Devolve o número de notas exportadas.
     */
    public int export(Path target, Long folderId, ThemeColors palette) throws Exception {
        List<Note> notes = noteRepo.list(NoteRepository.Query.all().withFolder(folderId));
        if (notes.isEmpty()) return 0;

        Files.createDirectories(target);
        Files.writeString(target.resolve("style.css"), css(palette), StandardCharsets.UTF_8);

        Map<Long, String> folderNames = new LinkedHashMap<>();
        for (Folder f : folderRepo.list()) folderNames.put(f.getId(), f.getName());

        // resolve wikilinks pelo título: mapa título → ficheiro
        Map<String, String> filesByTitle = new LinkedHashMap<>();
        Map<Long, String> filesById = new LinkedHashMap<>();
        for (Note n : notes) {
            String file = slug(n.displayTitle()) + "-" + n.getId() + ".html";
            filesById.put(n.getId(), file);
            filesByTitle.putIfAbsent(n.displayTitle().toLowerCase(Locale.ROOT), file);
        }

        boolean copiedAttachments = false;
        for (Note n : notes) {
            String body = renderer.render(parser.parse(rewriteWikilinks(n.getContent(), filesByTitle)));
            if (body.contains("attachment:")) {
                if (!copiedAttachments) {
                    copyAttachments(target);
                    copiedAttachments = true;
                }
                body = rewriteAttachments(body);
            }
            String folder = n.getFolderId() == null ? null : folderNames.get(n.getFolderId());
            Files.writeString(target.resolve(filesById.get(n.getId())),
                    page(n, folder, body), StandardCharsets.UTF_8);
        }

        Files.writeString(target.resolve("index.html"),
                index(notes, folderNames, filesById), StandardCharsets.UTF_8);
        return notes.size();
    }

    // ---------------------------------------------------------------- páginas

    private String page(Note note, String folder, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(head(note.displayTitle()));
        sb.append("<nav class=\"crumbs\"><a href=\"index.html\">Índice</a>");
        if (folder != null) sb.append(" › ").append(escape(folder));
        sb.append(" › ").append(escape(note.displayTitle())).append("</nav>\n");
        sb.append("<h1>").append(escape(note.displayTitle())).append("</h1>\n");
        if (!note.getTags().isEmpty()) {
            sb.append("<p class=\"tags\">");
            for (String tag : note.getTags()) sb.append("<span>#").append(escape(tag)).append("</span> ");
            sb.append("</p>\n");
        }
        sb.append("<article>").append(body).append("</article>\n");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String index(List<Note> notes, Map<Long, String> folderNames, Map<Long, String> files) {
        Map<String, List<Note>> byFolder = new LinkedHashMap<>();
        for (Note n : notes) {
            String key = n.getFolderId() == null ? "Sem pasta"
                    : folderNames.getOrDefault(n.getFolderId(), "Sem pasta");
            byFolder.computeIfAbsent(key, k -> new ArrayList<>()).add(n);
        }

        StringBuilder sb = new StringBuilder(head("Jotes"));
        sb.append("<h1>Jotes</h1>\n<p class=\"tags\"><span>")
                .append(notes.size()).append(" notas</span></p>\n");
        for (Map.Entry<String, List<Note>> entry : byFolder.entrySet()) {
            sb.append("<h2>").append(escape(entry.getKey())).append("</h2>\n<ul>\n");
            for (Note n : entry.getValue()) {
                sb.append("  <li><a href=\"").append(files.get(n.getId())).append("\">")
                        .append(escape(n.displayTitle())).append("</a></li>\n");
            }
            sb.append("</ul>\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String head(String title) {
        return "<!doctype html>\n<html lang=\"pt\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + escape(title) + "</title>"
                + "<link rel=\"stylesheet\" href=\"style.css\"></head><body>\n";
    }

    // ---------------------------------------------------------------- reescritas

    /** Converte {@code [[Título]]} em {@code <a href="ficheiro.html">}. */
    private String rewriteWikilinks(String markdown, Map<String, String> filesByTitle) {
        Matcher m = WIKILINK.matcher(markdown == null ? "" : markdown);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String title = m.group(1).trim();
            String alias = m.group(2) == null ? "" : m.group(2).trim();
            String text = alias.isEmpty() ? title : alias;
            String file = filesByTitle.get(title.toLowerCase(Locale.ROOT));
            // nota inexistente: fica texto simples, não um link partido
            String replacement = file == null ? text : "[" + text + "](" + file + ")";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String rewriteAttachments(String html) {
        Matcher m = ATTACH_ATTR.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String rel = m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    m.group(1) + "=\"attachments/" + rel + "\""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void copyAttachments(Path target) throws IOException {
        Path source = storage.root();
        if (!Files.isDirectory(source)) return;
        Path dest = target.resolve("attachments");
        try (var walk = Files.walk(source)) {
            for (Path p : walk.toList()) {
                Path rel = source.relativize(p);
                Path out = dest.resolve(rel.toString());
                if (Files.isDirectory(p)) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    Files.copy(p, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /** Nome de ficheiro seguro a partir do título (ASCII, minúsculas, com hífenes). */
    public static String slug(String title) {
        String normalized = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.isBlank()) normalized = "nota";
        return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Folha de estilo derivada da paleta ativa, para o site sair com o mesmo aspeto. */
    private static String css(ThemeColors p) {
        return """
                :root {
                  --bg: %s; --card: %s; --text: %s; --dim: %s;
                  --accent: %s; --sep: %s;
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0 auto; max-width: 46rem; padding: 2.5rem 1.25rem 4rem;
                  background: var(--bg); color: var(--text);
                  font: 16px/1.65 -apple-system, "Segoe UI", Roboto, sans-serif;
                }
                a { color: var(--accent); text-decoration: none; }
                a:hover { text-decoration: underline; }
                h1 { font-size: 2rem; margin: .4rem 0 1rem; }
                h2 { font-size: 1.25rem; margin-top: 2rem; }
                nav.crumbs { color: var(--dim); font-size: .8rem; margin-bottom: 1.5rem; }
                p.tags span {
                  display: inline-block; background: var(--card); color: var(--dim);
                  border-radius: 999px; padding: .1rem .6rem; font-size: .75rem; margin-right: .3rem;
                }
                ul { padding-left: 1.1rem; }
                li { margin: .3rem 0; }
                article img { max-width: 100%%; border-radius: 8px; }
                code, pre { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .9em; }
                code { background: var(--card); border-radius: 4px; padding: .1rem .3rem; }
                pre { background: var(--card); border: 1px solid var(--sep); border-radius: 10px;
                      padding: .9rem 1rem; overflow-x: auto; }
                pre code { background: none; padding: 0; }
                blockquote { border-left: 3px solid var(--sep); color: var(--dim);
                             margin: 1rem 0; padding: .1rem 0 .1rem 1rem; }
                table { border-collapse: collapse; width: 100%%; }
                th, td { border: 1px solid var(--sep); padding: .4rem .7rem; text-align: left; }
                th { background: var(--card); }
                hr { border: none; border-top: 1px solid var(--sep); margin: 2rem 0; }
                """.formatted(hex(p.bg()), hex(p.cardBg()), hex(p.text()),
                hex(p.textDim()), hex(p.accent()), hex(p.separator()));
    }
}
