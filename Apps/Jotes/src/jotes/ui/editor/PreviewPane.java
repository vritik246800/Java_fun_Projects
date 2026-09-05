package jotes.ui.editor;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.misc.Extension;
import jotes.services.AttachmentStorage;
import jotes.ui.Theme;
import jotes.ui.anim.SmoothScroll;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pré-visualização HTML da nota. O markdown é renderizado com flexmark; antes de
 * mostrar, os links internos {@code [[Título]]} viram links {@code jotes:} e as
 * referências {@code attachment:...} são reescritas para URLs {@code file:} absolutos.
 * <p>Além disso: as caixas {@code - [ ]} ficam clicáveis (marcam no markdown), os
 * blocos de código levam realce de sintaxe ({@link CodeHighlighter}) e cada tabela
 * ganha um link "editar". O HTML gerado é guardado em cache, para não re-renderizar
 * a cada tecla quando o texto não mudou.</p>
 */
public class PreviewPane extends JPanel {

    private static final Pattern WIKILINK =
            Pattern.compile("\\[\\[([^\\[\\]|]+?)(?:\\|([^\\[\\]]*?))?\\]\\]");
    private static final Pattern ATTACH_ATTR =
            Pattern.compile("(src|href)=\"attachment:([^\"]+)\"");
    /** Item de lista de tarefas no início de uma linha, com a indentação preservada. */
    private static final Pattern TASK_ITEM =
            Pattern.compile("^(\\s*(?:[-*+]|\\d+\\.)\\s+)\\[([ xX])\\]\\s?(.*)$");
    /** Bloco de código cercado, com linguagem opcional. */
    private static final Pattern FENCED_CODE =
            Pattern.compile("(?m)^```([\\w+#.-]*)\\s*\\n(.*?)^```\\s*$", Pattern.DOTALL);
    private static final Pattern RENDERED_CODE =
            Pattern.compile("<pre><code(?: class=\"language-([^\"]*)\")?>(.*?)</code></pre>", Pattern.DOTALL);

    private static final List<Extension> EXTENSIONS =
            List.of(TablesExtension.create(), StrikethroughExtension.create());

    private final AttachmentStorage storage;
    private final JEditorPane pane = new JEditorPane();
    private final JScrollPane scroll;
    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    // cache do último render: evita reconstruir o HTML a cada tecla
    private String cachedMarkdown;
    private boolean cachedDark;
    private String cachedHtml;

    private Consumer<String> onNavigateToTitle = t -> {};
    private IntConsumer onToggleTask = i -> {};
    private IntConsumer onEditTable = i -> {};

    public PreviewPane(AttachmentStorage storage) {
        super(new BorderLayout());
        this.storage = storage;
        setBackground(Theme.BG);

        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(Theme.BG);
        pane.setForeground(Theme.TEXT);
        pane.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        pane.addHyperlinkListener(this::onHyperlink);
        scroll = new JScrollPane(pane);
        Theme.styleScrollPane(scroll);
        SmoothScroll.install(scroll);
        add(scroll, BorderLayout.CENTER);
    }

    /** Listener para cliques em links internos ({@code jotes:título}). */
    public void setOnNavigateToTitle(Consumer<String> listener) {
        this.onNavigateToTitle = listener == null ? t -> {} : listener;
    }

    /** Chamado com o índice (0-based) da caixa de tarefa clicada na pré-visualização. */
    public void setOnToggleTask(IntConsumer listener) {
        this.onToggleTask = listener == null ? i -> {} : listener;
    }

    /** Chamado com o índice (0-based) da tabela cujo link "editar" foi clicado. */
    public void setOnEditTable(IntConsumer listener) {
        this.onEditTable = listener == null ? i -> {} : listener;
    }

    /** O scroll da pré-visualização, para sincronizar com o do editor. */
    public JScrollPane scrollPane() {
        return scroll;
    }

    /** Posição do scroll como fração de 0 a 1. */
    public double scrollFraction() {
        JScrollBar bar = scroll.getVerticalScrollBar();
        int span = bar.getMaximum() - bar.getVisibleAmount() - bar.getMinimum();
        return span <= 0 ? 0 : (bar.getValue() - bar.getMinimum()) / (double) span;
    }

    /** Coloca o scroll na fração indicada (0 = topo, 1 = fundo). */
    public void setScrollFraction(double fraction) {
        JScrollBar bar = scroll.getVerticalScrollBar();
        int span = bar.getMaximum() - bar.getVisibleAmount() - bar.getMinimum();
        if (span <= 0) return;
        bar.setValue(bar.getMinimum() + (int) Math.round(Math.max(0, Math.min(1, fraction)) * span));
    }

    /** Re-aplica as cores do tema quando a paleta muda (chamado por updateComponentTreeUI). */
    @Override
    public void updateUI() {
        super.updateUI();
        if (pane == null) return; // chamado pelo construtor do JPanel
        setBackground(Theme.BG);
        pane.setBackground(Theme.BG);
        pane.setForeground(Theme.TEXT);
        cachedHtml = null; // o CSS embutido depende da paleta
    }

    /**
     * Renderiza o markdown e mostra o resultado do topo. Se o texto e o tema não
     * mudaram desde a última vez, reaproveita o HTML em cache e não mexe no scroll.
     */
    public void setMarkdown(String markdown) {
        String source = markdown == null ? "" : markdown;
        if (cachedHtml != null && source.equals(cachedMarkdown) && cachedDark == Theme.isDark()) {
            return;
        }
        cachedMarkdown = source;
        cachedDark = Theme.isDark();
        cachedHtml = render(source);
        pane.setText(cachedHtml);
        pane.setCaretPosition(0);
    }

    /** Descarta a cache; o próximo {@link #setMarkdown(String)} volta a renderizar. */
    public void invalidateCache() {
        cachedHtml = null;
    }

    private String render(String markdown) {
        String prepared = preprocessTasks(preprocessLinks(markdown));
        Node doc = parser.parse(prepared);
        String html = renderer.render(doc);
        html = highlightCode(html, markdown);
        html = addTableEditLinks(html);
        return wrapHtml(resolveAttachments(html));
    }

    // ---------------------------------------------------------------- pré-processamento

    /** Converte {@code [[Título]]} / {@code [[Título|texto]]} em links markdown {@code jotes:}. */
    static String preprocessLinks(String markdown) {
        Matcher m = WIKILINK.matcher(markdown);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String title = m.group(1).trim();
            String alias = m.group(2) == null ? "" : m.group(2).trim();
            String text = alias.isEmpty() ? title : alias;
            String link = "[" + text + "](jotes:" + title.replace(" ", "%20") + ")";
            m.appendReplacement(sb, Matcher.quoteReplacement(link));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Troca cada {@code - [ ]} / {@code - [x]} por um link {@code jotes-task:<n>},
     * onde {@code n} é a ordem da tarefa no documento. O JEditorPane não deixa
     * clicar em {@code <input type=checkbox>}, mas deixa clicar num link.
     */
    static String preprocessTasks(String markdown) {
        StringBuilder out = new StringBuilder(markdown.length() + 64);
        int index = 0;
        boolean inCode = false;
        String[] lines = markdown.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith("```")) inCode = !inCode;
            Matcher m = inCode ? null : TASK_ITEM.matcher(line);
            if (m != null && m.matches()) {
                boolean checked = !m.group(2).isBlank();
                out.append(m.group(1))
                        .append("[").append(checked ? "☑" : "☐").append("](jotes-task:").append(index++).append(") ")
                        .append(m.group(3));
            } else {
                out.append(line);
            }
            if (i < lines.length - 1) out.append('\n');
        }
        return out.toString();
    }

    /** Índices (0-based) das linhas do markdown que são itens de tarefa. */
    public static List<Integer> taskLines(String markdown) {
        List<Integer> out = new ArrayList<>();
        boolean inCode = false;
        String[] lines = (markdown == null ? "" : markdown).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (!inCode && TASK_ITEM.matcher(lines[i]).matches()) out.add(i);
        }
        return out;
    }

    /**
     * Inverte o estado da n-ésima tarefa do markdown e devolve o texto atualizado.
     * Índice fora do intervalo devolve o texto tal como estava.
     */
    public static String toggleTask(String markdown, int index) {
        List<Integer> lines = taskLines(markdown);
        if (index < 0 || index >= lines.size()) return markdown;
        String[] all = markdown.split("\n", -1);
        int lineNumber = lines.get(index);
        Matcher m = TASK_ITEM.matcher(all[lineNumber]);
        if (!m.matches()) return markdown;
        boolean checked = !m.group(2).isBlank();
        all[lineNumber] = m.group(1) + "[" + (checked ? " " : "x") + "] " + m.group(3);
        return String.join("\n", all);
    }

    // ---------------------------------------------------------------- pós-processamento

    /**
     * Substitui o conteúdo dos blocos {@code <pre><code>} pelo mesmo código já
     * colorido. A linguagem vem da classe gerada pelo flexmark; quando falta,
     * usa-se a ordem dos blocos cercados do markdown original.
     */
    private String highlightCode(String html, String markdown) {
        List<String> languages = new ArrayList<>();
        Matcher fences = FENCED_CODE.matcher(markdown);
        while (fences.find()) languages.add(fences.group(1));

        Matcher m = RENDERED_CODE.matcher(html);
        StringBuilder sb = new StringBuilder();
        int block = 0;
        while (m.find()) {
            String language = m.group(1);
            if ((language == null || language.isBlank()) && block < languages.size()) {
                language = languages.get(block);
            }
            block++;
            String code = unescape(m.group(2));
            String highlighted = CodeHighlighter.highlight(code, language);
            m.appendReplacement(sb, Matcher.quoteReplacement("<pre><code>" + highlighted + "</code></pre>"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Põe um link "editar" por cima de cada tabela renderizada. */
    private String addTableEditLinks(String html) {
        StringBuilder sb = new StringBuilder(html.length() + 64);
        int from = 0;
        int index = 0;
        while (true) {
            int at = html.indexOf("<table", from);
            if (at < 0) break;
            sb.append(html, from, at);
            sb.append("<p class=\"tableedit\"><a href=\"jotes-table:").append(index++)
                    .append("\">editar tabela</a></p>");
            sb.append("<table");
            from = at + "<table".length(); // avança para lá da etiqueta, senão volta a encontrá-la
        }
        sb.append(html.substring(from));
        return sb.toString();
    }

    /** Reescreve atributos src/href "attachment:relpath" para URLs file: absolutos. */
    private String resolveAttachments(String html) {
        Matcher m = ATTACH_ATTR.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String rel = m.group(2).replace("%20", " ");
            String url = storage.resolve(rel).toUri().toString();
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "=\"" + url + "\""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Envolve o corpo com CSS derivado das cores do {@link Theme} (paleta ativa). */
    private static String wrapHtml(String body) {
        return "<html><head><style>"
                + "body { font-family: sans-serif; font-size: 14px; margin: 4px;"
                + " background-color: " + hex(Theme.BG) + "; color: " + hex(Theme.TEXT) + "; }"
                + "a { color: " + hex(Theme.ACCENT) + "; }"
                + "code, pre { font-family: monospace; background-color: " + hex(Theme.CARD_BG) + "; }"
                + "pre { padding: 8px; border: 1px solid " + hex(Theme.SEPARATOR) + "; }"
                + "blockquote { color: " + hex(Theme.TEXT_DIM) + "; margin-left: 6px; padding-left: 10px; }"
                + "table, th, td { border: 1px solid " + hex(Theme.SEPARATOR)
                + "; border-collapse: collapse; padding: 4px 8px; }"
                + "th { background-color: " + hex(Theme.CARD_BG) + "; }"
                + ".tableedit { font-size: 10px; margin: 6px 0 2px 0; }"
                + ".tableedit a { color: " + hex(Theme.TEXT_DIM) + "; }"
                + "</style></head><body>" + body + "</body></html>";
    }

    /** Cor em {@code #rrggbb} para o CSS do HTML renderizado pelo JEditorPane. */
    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
    }

    private void onHyperlink(HyperlinkEvent e) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
        String href = e.getDescription();
        if ((href == null || href.isEmpty()) && e.getURL() != null) href = e.getURL().toString();
        if (href == null) return;
        if (href.startsWith("jotes-task:")) {
            onToggleTask.accept(parseIndex(href.substring("jotes-task:".length())));
        } else if (href.startsWith("jotes-table:")) {
            onEditTable.accept(parseIndex(href.substring("jotes-table:".length())));
        } else if (href.startsWith("jotes:")) {
            onNavigateToTitle.accept(href.substring("jotes:".length()).replace("%20", " "));
        } else if (href.startsWith("http://") || href.startsWith("https://")) {
            if (Desktop.isDesktopSupported()) {
                try { Desktop.getDesktop().browse(new URI(href)); } catch (Exception ignored) {}
            }
        } else if (href.startsWith("file:")) {
            if (Desktop.isDesktopSupported()) {
                try { Desktop.getDesktop().open(Path.of(new URI(href)).toFile()); } catch (Exception ignored) {}
            }
        }
    }

    private static int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
