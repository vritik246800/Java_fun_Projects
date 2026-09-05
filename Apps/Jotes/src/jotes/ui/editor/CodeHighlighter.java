package jotes.ui.editor;

import jotes.ui.Theme;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

import javax.swing.text.Segment;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Realce de sintaxe para os blocos de código da pré-visualização. Reaproveita o
 * tokenizador do RSyntaxTextArea (já no classpath pelo editor) sobre um
 * {@link RSyntaxDocument} fora do ecrã e devolve HTML com {@code <span>} coloridos
 * a partir da paleta do {@link Theme} — sem depender de nenhuma biblioteca nova.
 */
public final class CodeHighlighter {

    /** Nome da linguagem no bloco markdown → estilo do RSyntaxTextArea. */
    private static final Map<String, String> LANGUAGES = new HashMap<>();

    static {
        LANGUAGES.put("java", SyntaxConstants.SYNTAX_STYLE_JAVA);
        LANGUAGES.put("js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        LANGUAGES.put("javascript", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        LANGUAGES.put("ts", SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
        LANGUAGES.put("typescript", SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
        LANGUAGES.put("py", SyntaxConstants.SYNTAX_STYLE_PYTHON);
        LANGUAGES.put("python", SyntaxConstants.SYNTAX_STYLE_PYTHON);
        LANGUAGES.put("c", SyntaxConstants.SYNTAX_STYLE_C);
        LANGUAGES.put("cpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        LANGUAGES.put("c++", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        LANGUAGES.put("cs", SyntaxConstants.SYNTAX_STYLE_CSHARP);
        LANGUAGES.put("csharp", SyntaxConstants.SYNTAX_STYLE_CSHARP);
        LANGUAGES.put("html", SyntaxConstants.SYNTAX_STYLE_HTML);
        LANGUAGES.put("xml", SyntaxConstants.SYNTAX_STYLE_XML);
        LANGUAGES.put("css", SyntaxConstants.SYNTAX_STYLE_CSS);
        LANGUAGES.put("json", SyntaxConstants.SYNTAX_STYLE_JSON);
        LANGUAGES.put("yaml", SyntaxConstants.SYNTAX_STYLE_YAML);
        LANGUAGES.put("yml", SyntaxConstants.SYNTAX_STYLE_YAML);
        LANGUAGES.put("sql", SyntaxConstants.SYNTAX_STYLE_SQL);
        LANGUAGES.put("sh", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        LANGUAGES.put("bash", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        LANGUAGES.put("shell", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        LANGUAGES.put("php", SyntaxConstants.SYNTAX_STYLE_PHP);
        LANGUAGES.put("rb", SyntaxConstants.SYNTAX_STYLE_RUBY);
        LANGUAGES.put("ruby", SyntaxConstants.SYNTAX_STYLE_RUBY);
        LANGUAGES.put("go", SyntaxConstants.SYNTAX_STYLE_GO);
        LANGUAGES.put("rs", SyntaxConstants.SYNTAX_STYLE_RUST);
        LANGUAGES.put("rust", SyntaxConstants.SYNTAX_STYLE_RUST);
        LANGUAGES.put("kt", SyntaxConstants.SYNTAX_STYLE_KOTLIN);
        LANGUAGES.put("kotlin", SyntaxConstants.SYNTAX_STYLE_KOTLIN);
        LANGUAGES.put("md", SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        LANGUAGES.put("dockerfile", SyntaxConstants.SYNTAX_STYLE_DOCKERFILE);
        LANGUAGES.put("properties", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
    }

    private CodeHighlighter() {}

    /** {@code true} se houver realce para esta linguagem. */
    public static boolean supports(String language) {
        return language != null && LANGUAGES.containsKey(language.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Devolve o código já em HTML, com as palavras coloridas por tipo de token.
     * Linguagem desconhecida ou vazia devolve o código apenas escapado.
     */
    public static String highlight(String code, String language) {
        String style = language == null ? null : LANGUAGES.get(language.trim().toLowerCase(Locale.ROOT));
        if (style == null) return escape(code);

        RSyntaxDocument doc = new RSyntaxDocument(style);
        StringBuilder out = new StringBuilder(code.length() * 2);
        try {
            doc.insertString(0, code, null);
            for (int line = 0; line < doc.getDefaultRootElement().getElementCount(); line++) {
                if (line > 0) out.append('\n');
                Token token = doc.getTokenListForLine(line);
                while (token != null && token.isPaintable()) {
                    appendToken(out, token);
                    token = token.getNextToken();
                }
            }
        } catch (Exception e) {
            // tokenização falhada: melhor código simples do que preview partido
            return escape(code);
        }
        return out.toString();
    }

    private static void appendToken(StringBuilder out, Token token) {
        Segment segment = new Segment(token.getTextArray(), token.getTextOffset(), token.length());
        String text = escape(segment.toString());
        Color color = colorOf(token.getType());
        if (color == null) {
            out.append(text);
        } else {
            out.append("<span style=\"color:").append(hex(color)).append("\">").append(text).append("</span>");
        }
    }

    /** Cor por família de token, derivada da paleta ativa para funcionar nos dois temas. */
    private static Color colorOf(int type) {
        return switch (type) {
            case TokenTypes.COMMENT_EOL, TokenTypes.COMMENT_MULTILINE,
                 TokenTypes.COMMENT_DOCUMENTATION, TokenTypes.COMMENT_KEYWORD,
                 TokenTypes.COMMENT_MARKUP -> Theme.TEXT_DIM;
            case TokenTypes.RESERVED_WORD, TokenTypes.RESERVED_WORD_2 -> Theme.ACCENT;
            case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, TokenTypes.LITERAL_CHAR,
                 TokenTypes.LITERAL_BACKQUOTE -> mix(Theme.ACCENT, Theme.TEXT, 0.45f);
            case TokenTypes.LITERAL_NUMBER_DECIMAL_INT, TokenTypes.LITERAL_NUMBER_FLOAT,
                 TokenTypes.LITERAL_NUMBER_HEXADECIMAL, TokenTypes.LITERAL_BOOLEAN -> Theme.DANGER;
            case TokenTypes.FUNCTION, TokenTypes.MARKUP_TAG_NAME -> mix(Theme.TEXT, Theme.ACCENT, 0.35f);
            case TokenTypes.ANNOTATION, TokenTypes.PREPROCESSOR,
                 TokenTypes.MARKUP_TAG_ATTRIBUTE -> Theme.TEXT_DIM;
            case TokenTypes.DATA_TYPE, TokenTypes.VARIABLE -> mix(Theme.TEXT, Theme.ACCENT, 0.2f);
            case TokenTypes.OPERATOR, TokenTypes.SEPARATOR -> Theme.TEXT_DIM;
            default -> null; // texto normal herda a cor do bloco
        };
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
