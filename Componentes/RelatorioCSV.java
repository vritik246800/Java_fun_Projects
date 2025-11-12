import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Tabela_Matriz_Adjacencia extends JFrame {
    private int[][] matriz;
    private String[] cidades;

    public Tabela_Matriz_Adjacencia(int[][] matriz, String[] cidades) {
        this.matriz = matriz;
        this.cidades = cidades;

        setTitle("📊 Matriz de Adjacência");
        setSize(1800, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---------- TABELA ----------
        String[] columnNames = new String[cidades.length + 1];
        columnNames[0] = "Cidades";
        System.arraycopy(cidades, 0, columnNames, 1, cidades.length);

        Object[][] data = new Object[cidades.length][cidades.length + 1];
        for (int i = 0; i < cidades.length; i++) {
            data[i][0] = cidades[i];
            for (int j = 0; j < cidades.length; j++) {
                data[i][j + 1] = matriz[i][j];
            }
        }

        JTable table = new JTable(data, columnNames);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        // Configurar cabeçalho da tabela com renderer customizado (NOSSO - corrige visibilidade)
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        header.setBackground(new Color(101, 44, 194));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value.toString());
                label.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
                label.setBackground(new Color(101, 44, 194));
                label.setForeground(Color.WHITE);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return label;
            }
        });

        table.setGridColor(new Color(200, 200, 200));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setEnabled(false);

        // Renderer personalizado para células
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                        boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 0) {
                    setBackground(new Color(240, 240, 240));
                    setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
                    setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                    setHorizontalAlignment(SwingConstants.CENTER);
                    if (value instanceof Integer && (Integer) value > 0) {
                        setBackground(new Color(149, 104, 229));
                        setForeground(Color.WHITE);
                    } else {
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                }
                if (isSelected) setBackground(new Color(184, 207, 229));
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // ---------- AJUSTE AUTOMÁTICO DE COLUNAS ----------
        ajustarLarguraColunas(table);

        // Atualiza automaticamente ao redimensionar a janela
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ajustarLarguraColunas(table);
            }
        });

        // ---------- MENU DE CLIQUE DIREITO (VRITIK - export CSV) ----------
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem itemExportar = new JMenuItem("💾 Exportar Matriz para CSV");
        itemExportar.addActionListener(e -> exportarCSV());
        popupMenu.add(itemExportar);

        JMenuItem itemFechar = new JMenuItem("❌ Fechar Janela");
        itemFechar.addActionListener(e -> dispose());
        popupMenu.add(itemFechar);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // ---------- SCROLL ----------
        JScrollPane scrollTabela = new JScrollPane(table);
        scrollTabela.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollTabela.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollTabela.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollTabela, BorderLayout.CENTER);
        setVisible(true);
    }

    /**
     * Ajusta automaticamente a largura das colunas da JTable com base na largura da janela
     * e no conteúdo (modo proporcional + medição do conteúdo real).
     */
    private void ajustarLarguraColunas(JTable table) {
        if (table == null || table.getColumnCount() == 0) return;

        int totalWidth = getWidth() - 80; // largura útil da janela
        if (totalWidth <= 200) totalWidth = 800;

        int numCols = table.getColumnCount();
        int larguraBase = totalWidth / numCols;

        for (int i = 0; i < numCols; i++) {
            int preferredWidth = larguraBase;
            if (i == 0) preferredWidth += 100; // primeira coluna maior

            TableColumn col = table.getColumnModel().getColumn(i);
            int headerWidth = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, i)
                    .getPreferredSize().width;
            int maxCellWidth = headerWidth;

            int rowsToCheck = Math.min(table.getRowCount(), 20);
            for (int r = 0; r < rowsToCheck; r++) {
                Object val = table.getValueAt(r, i);
                if (val != null) {
                    Component comp = table.getCellRenderer(r, i)
                            .getTableCellRendererComponent(table, val, false, false, r, i);
                    maxCellWidth = Math.max(maxCellWidth, comp.getPreferredSize().width);
                }
            }

            int chosen = Math.max(preferredWidth, maxCellWidth + 20);
            chosen = Math.max(80, Math.min(chosen, totalWidth - (numCols - 1) * 80));
            col.setPreferredWidth(chosen);
        }

        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }


    /**
     * Exporta a matriz para CSV usando JFileChooser.
     */
    private void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Escolha o local para guardar a matriz CSV");
        fileChooser.setSelectedFile(new File("matriz_adjacencia.csv"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(fileToSave))) {
                // Cabeçalho
                pw.print("Cidades");
                for (String cidade : cidades) pw.print("," + cidade);
                pw.println();

                // Linhas da matriz
                for (int i = 0; i < cidades.length; i++) {
                    pw.print(cidades[i]);
                    for (int j = 0; j < cidades.length; j++) {
                        pw.print("," + matriz[i][j]);
                    }
                    pw.println();
                }

                JOptionPane.showMessageDialog(this,
                        "✅ Matriz exportada com sucesso!\n\nFicheiro: " + fileToSave.getAbsolutePath(),
                        "Exportação Concluída", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "❌ Erro ao exportar CSV:\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
