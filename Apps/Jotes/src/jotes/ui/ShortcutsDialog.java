package jotes.ui;

import jotes.ui.anim.SmoothScroll;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Tela de ajuda e edição de atalhos (Ctrl+/): lista todos os comandos registados
 * em {@link Shortcuts} e permite mudar o atalho de cada um — duplo-clique na linha,
 * depois a combinação de teclas pretendida.
 */
public class ShortcutsDialog extends JDialog {

    private final Shortcuts shortcuts;
    private final ShortcutTableModel model;
    private final JTable table;
    private final JLabel hint = new JLabel(
            "Duplo-clique num atalho e prime a nova combinação. Backspace remove; Esc cancela.",
            SwingConstants.LEFT);

    public ShortcutsDialog(Window owner, Shortcuts shortcuts) {
        super(owner, "Atalhos de teclado", ModalityType.APPLICATION_MODAL);
        this.shortcuts = shortcuts;
        this.model = new ShortcutTableModel(shortcuts.commands());
        this.table = new JTable(model);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(620, 520);
        setLocationRelativeTo(owner);

        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setBackground(Theme.BG);
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(Theme.SELECTION);
        table.setSelectionForeground(Theme.TEXT);
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setBackground(Theme.SIDEBAR_BG);
        table.getTableHeader().setForeground(Theme.TEXT_DIM);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 11));
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);

        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setForeground(column == 2 ? Theme.ACCENT : (column == 0 ? Theme.TEXT_DIM : Theme.TEXT));
                setFont(Theme.font(column == 2 ? Font.BOLD : Font.PLAIN, 13));
                return this;
            }
        };
        for (int i = 0; i < 3; i++) table.getColumnModel().getColumn(i).setCellRenderer(cells);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) captureStroke();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        Theme.styleScrollPane(scroll);
        SmoothScroll.install(scroll);

        hint.setForeground(Theme.TEXT_DIM);
        hint.setFont(Theme.font(Font.PLAIN, 11));
        hint.setBorder(BorderFactory.createEmptyBorder(8, 2, 4, 2));

        JButton change = new JButton("Mudar atalho");
        Theme.styleButton(change);
        change.addActionListener(e -> captureStroke());

        JButton reset = new JButton("Repor omissões");
        Theme.styleButton(reset);
        reset.addActionListener(e -> {
            shortcuts.resetAll();
            model.reload(shortcuts.commands());
        });

        JButton close = new JButton("Fechar");
        Theme.styleButton(close);
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(change);
        buttons.add(reset);
        buttons.add(close);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(hint, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(scroll, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JPanel.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Abre um pequeno diálogo que captura a próxima combinação de teclas. */
    private void captureStroke() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Shortcuts.Command command = model.commandAt(row);

        JDialog capture = new JDialog(this, "Novo atalho", ModalityType.APPLICATION_MODAL);
        JLabel message = new JLabel("<html><center>Prime a combinação para<br><b>"
                + command.label() + "</b></center></html>", SwingConstants.CENTER);
        message.setForeground(Theme.TEXT);
        message.setFont(Theme.font(Font.PLAIN, 14));
        message.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG);
        panel.setBorder(BorderFactory.createLineBorder(Theme.SEPARATOR));
        panel.add(message, BorderLayout.CENTER);
        capture.setContentPane(panel);
        capture.setSize(320, 160);
        capture.setLocationRelativeTo(this);
        capture.setFocusable(true);

        capture.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    capture.dispose();
                    return;
                }
                if (code == KeyEvent.VK_BACK_SPACE) {
                    shortcuts.setStroke(command.id(), null);
                    model.reload(shortcuts.commands());
                    capture.dispose();
                    return;
                }
                // ignora as teclas modificadoras isoladas
                if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_SHIFT
                        || code == KeyEvent.VK_ALT || code == KeyEvent.VK_META) {
                    return;
                }
                KeyStroke stroke = KeyStroke.getKeyStroke(code, e.getModifiersEx());
                if (stroke.getModifiers() == 0 && code != KeyEvent.VK_F1) {
                    JOptionPane.showMessageDialog(capture,
                            "Usa pelo menos uma tecla modificadora (Ctrl, Alt, Shift ou Cmd).",
                            "Atalho inválido", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                shortcuts.setStroke(command.id(), stroke);
                model.reload(shortcuts.commands());
                capture.dispose();
            }
        });
        capture.setVisible(true);
    }

    /** Tabela de três colunas: grupo, comando, atalho. */
    private static final class ShortcutTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Secção", "Comando", "Atalho"};
        private List<Shortcuts.Command> rows;

        ShortcutTableModel(List<Shortcuts.Command> rows) {
            this.rows = rows;
        }

        void reload(List<Shortcuts.Command> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Shortcuts.Command commandAt(int row) {
            return rows.get(row);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public boolean isCellEditable(int row, int column) { return false; }

        @Override
        public Object getValueAt(int row, int column) {
            Shortcuts.Command c = rows.get(row);
            return switch (column) {
                case 0 -> c.group();
                case 1 -> c.label();
                default -> c.strokeText().isEmpty() ? "—" : c.strokeText();
            };
        }
    }
}
