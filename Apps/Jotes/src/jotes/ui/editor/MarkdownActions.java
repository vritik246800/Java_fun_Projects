package jotes.ui.editor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/** Ações de formatação markdown aplicadas à seleção/caret do editor de conteúdo. */
public final class MarkdownActions {

    private MarkdownActions() {}

    /**
     * Envolve a seleção com prefixo/sufixo (ex.: {@code **negrito**}).
     * Sem seleção, insere o par e deixa o cursor entre os marcadores.
     */
    public static void wrapSelection(RSyntaxTextArea area, String prefix, String suffix) {
        String sel = area.getSelectedText();
        if (sel == null) sel = "";
        int start = area.getSelectionStart();
        area.replaceSelection(prefix + sel + suffix);
        if (sel.isEmpty()) {
            area.setCaretPosition(start + prefix.length());
        } else {
            area.select(start + prefix.length(), start + prefix.length() + sel.length());
        }
        area.requestFocusInWindow();
    }

    /** Prefixa todas as linhas abrangidas pela seleção (ex.: {@code "- "}, {@code "# "}). */
    public static void prefixLines(RSyntaxTextArea area, String prefix) {
        Document doc = area.getDocument();
        Element root = doc.getDefaultRootElement();
        int start = area.getSelectionStart();
        int end = area.getSelectionEnd();
        int first = root.getElementIndex(start);
        // se a seleção termina no início de uma linha, essa linha fica de fora
        int last = root.getElementIndex(end > start ? end - 1 : end);
        try {
            // insere de baixo para cima para não invalidar os offsets das linhas seguintes
            for (int i = last; i >= first; i--) {
                doc.insertString(root.getElement(i).getStartOffset(), prefix, null);
            }
        } catch (BadLocationException ignored) {
        }
        area.requestFocusInWindow();
    }

    /** Código: bloco com fences se a seleção tiver várias linhas, senão backticks inline. */
    public static void wrapCode(RSyntaxTextArea area) {
        String sel = area.getSelectedText();
        if (sel != null && sel.contains("\n")) {
            wrapSelection(area, "```\n", "\n```");
        } else {
            wrapSelection(area, "`", "`");
        }
    }

    /** Insere um modelo de tabela 2x2 no caret (cabeçalho, separador e uma linha vazia). */
    public static void insertTable(RSyntaxTextArea area) {
        int pos = area.getCaretPosition();
        String table = "\n| Coluna 1 | Coluna 2 |\n"
                + "| -------- | -------- |\n"
                + "|          |          |\n";
        area.replaceSelection(table);
        area.setCaretPosition(pos + table.length());
        area.requestFocusInWindow();
    }

    /** Link interno {@code [[título]]}: envolve a seleção ou insere um placeholder selecionado. */
    public static void wrapInternalLink(RSyntaxTextArea area) {
        String sel = area.getSelectedText();
        if (sel == null || sel.isEmpty()) {
            int pos = area.getCaretPosition();
            area.replaceSelection("[[título]]");
            area.select(pos + 2, pos + 2 + "título".length());
            area.requestFocusInWindow();
        } else {
            wrapSelection(area, "[[", "]]");
        }
    }
}
