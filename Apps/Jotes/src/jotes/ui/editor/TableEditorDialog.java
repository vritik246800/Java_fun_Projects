package jotes.ui.editor;

import jotes.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Edita uma tabela markdown numa grelha. Serve a "tabela editável no preview":
 * clicar em "editar tabela" abre este diálogo com as células da tabela; ao guardar,
 * a tabela é reescrita no markdown mantendo a largura das colunas alinhada.
 */
public class TableEditorDialog extends JDialog {

    /** Uma tabela localizada dentro do markdown. */
    public record Located(int startLine, int endLine, List<List<String>> rows, List<String> alignments) {}

    private final DefaultTableModel model;
    private final List<String> alignments;
    private String result;

    public TableEditorDialog(Window owner, Located table) {
        super(owner, "Editar tabela", ModalityType.APPLICATION_MODAL);
        this.alignments = new ArrayList<>(table.alignments());

        List<List<String>> rows = table.rows();
        Object[] header = rows.get(0).toArray();
        Object[][] data = new Object[rows.size() - 1][];
        for (int i = 1; i < rows.size(); i++) data[i - 1] = rows.get(i).toArray();

        model = new DefaultTableModel(data, header);
        JTable grid = new JTable(model);
        grid.setRowHeight(26);
        grid.setBackground(Theme.BG);
        grid.setForeground(Theme.TEXT);
        grid.setGridColor(Theme.SEPARATOR);
        grid.setSelectionBackground(Theme.SELECTION);
        grid.setSelectionForeground(Theme.TEXT);
        grid.setFont(Theme.font(Font.PLAIN, 13));
        grid.getTableHeader().setBackground(Theme.CARD_BG);
        grid.getTableHeader().setForeground(Theme.TEXT);
        grid.getTableHeader().setFont(Theme.font(Font.BOLD, 12));
        grid.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JScrollPane scroll = new JScrollPane(grid);
        Theme.styleScrollPane(scroll);

        JButton addRow = new JButton("+ Linha");
        Theme.styleButton(addRow);
        addRow.addActionListener(e -> model.addRow(new Object[model.getColumnCount()]));

        JButton removeRow = new JButton("− Linha");
        Theme.styleButton(removeRow);
        removeRow.addActionListener(e -> {
            int row = grid.getSelectedRow();
            if (row >= 0) model.removeRow(row);
            else if (model.getRowCount() > 0) model.removeRow(model.getRowCount() - 1);
        });

        JButton addColumn = new JButton("+ Coluna");
        Theme.styleButton(addColumn);
        addColumn.addActionListener(e -> {
            model.addColumn("Coluna " + (model.getColumnCount() + 1));
            alignments.add("---");
        });

        JButton save = new JButton("Guardar");
        Theme.stylePrimaryButton(save);
        save.addActionListener(e -> {
            if (grid.isEditing()) grid.getCellEditor().stopCellEditing();
            result = toMarkdown();
            dispose();
        });

        JButton cancel = new JButton("Cancelar");
        Theme.styleButton(cancel);
        cancel.addActionListener(e -> dispose());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(addRow);
        left.add(removeRow);
        left.add(addColumn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(cancel);
        right.add(save);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttons.add(left, BorderLayout.WEST);
        buttons.add(right, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JPanel.WHEN_IN_FOCUSED_WINDOW);
        setSize(720, 420);
        setLocationRelativeTo(owner);
    }

    /** O markdown da tabela editada, ou {@code null} se o utilizador cancelou. */
    public String result() {
        return result;
    }

    /** Reescreve a grelha como tabela markdown, com as colunas alinhadas em largura. */
    private String toMarkdown() {
        int columns = model.getColumnCount();
        List<List<String>> rows = new ArrayList<>();

        List<String> header = new ArrayList<>();
        for (int c = 0; c < columns; c++) header.add(model.getColumnName(c));
        rows.add(header);
        for (int r = 0; r < model.getRowCount(); r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < columns; c++) {
                Object value = model.getValueAt(r, c);
                row.add(value == null ? "" : value.toString().replace("|", "\\|").trim());
            }
            rows.add(row);
        }

        int[] widths = new int[columns];
        for (List<String> row : rows) {
            for (int c = 0; c < columns; c++) widths[c] = Math.max(widths[c], row.get(c).length());
        }
        for (int c = 0; c < columns; c++) widths[c] = Math.max(3, widths[c]);

        StringBuilder sb = new StringBuilder();
        sb.append(renderRow(rows.get(0), widths));
        sb.append(renderSeparator(widths));
        for (int r = 1; r < rows.size(); r++) sb.append(renderRow(rows.get(r), widths));
        return sb.toString().stripTrailing();
    }

    private String renderRow(List<String> row, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int c = 0; c < widths.length; c++) {
            sb.append(' ').append(pad(row.get(c), widths[c])).append(" |");
        }
        return sb.append('\n').toString();
    }

    private String renderSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int c = 0; c < widths.length; c++) {
            String align = c < alignments.size() ? alignments.get(c) : "---";
            boolean left = align.startsWith(":");
            boolean right = align.endsWith(":");
            int dashes = widths[c] - (left ? 1 : 0) - (right ? 1 : 0);
            sb.append(' ').append(left ? ":" : "").append("-".repeat(Math.max(3, dashes)))
                    .append(right ? ":" : "").append(" |");
        }
        return sb.append('\n').toString();
    }

    private static String pad(String s, int width) {
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }

    // ---------------------------------------------------------------- localização

    /**
     * Encontra a n-ésima tabela markdown do texto (0-based). Uma tabela é uma
     * linha de cabeçalho com {@code |} seguida de uma linha de separadores.
     * Devolve {@code null} se não existir.
     */
    public static Located find(String markdown, int index) {
        String[] lines = (markdown == null ? "" : markdown).split("\n", -1);
        int found = 0;
        for (int i = 0; i + 1 < lines.length; i++) {
            if (!isRow(lines[i]) || !isSeparator(lines[i + 1])) continue;
            int end = i + 2;
            while (end < lines.length && isRow(lines[end])) end++;
            if (found++ == index) {
                List<List<String>> rows = new ArrayList<>();
                rows.add(cells(lines[i]));
                for (int r = i + 2; r < end; r++) rows.add(cells(lines[r]));
                List<String> alignments = cells(lines[i + 1]);
                normalize(rows, alignments.size());
                return new Located(i, end - 1, rows, alignments);
            }
            i = end - 1;
        }
        return null;
    }

    /** Substitui as linhas da tabela localizada pelo markdown novo. */
    public static String replace(String markdown, Located table, String replacement) {
        String[] lines = markdown.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == table.startLine()) {
                sb.append(replacement).append('\n');
            }
            if (i >= table.startLine() && i <= table.endLine()) continue;
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append('\n');
        }
        return sb.toString();
    }

    private static boolean isRow(String line) {
        String t = line.trim();
        return t.startsWith("|") && t.length() > 1;
    }

    private static boolean isSeparator(String line) {
        String t = line.trim();
        if (!t.startsWith("|")) return false;
        for (String cell : cells(line)) {
            if (!cell.matches(":?-{3,}:?")) return false;
        }
        return true;
    }

    private static List<String> cells(String line) {
        String t = line.trim();
        if (t.startsWith("|")) t = t.substring(1);
        if (t.endsWith("|")) t = t.substring(0, t.length() - 1);
        List<String> out = new ArrayList<>();
        for (String cell : t.split("(?<!\\\\)\\|", -1)) out.add(cell.replace("\\|", "|").trim());
        return out;
    }

    /** Garante que todas as linhas têm o mesmo número de colunas do cabeçalho. */
    private static void normalize(List<List<String>> rows, int columns) {
        int width = Math.max(columns, rows.get(0).size());
        for (List<String> row : rows) {
            while (row.size() < width) row.add("");
            while (row.size() > width) row.remove(row.size() - 1);
        }
    }
}
