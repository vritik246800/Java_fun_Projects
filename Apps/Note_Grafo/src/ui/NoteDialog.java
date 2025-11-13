package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Editor de notas modernizado com design profissional
 */
public class NoteDialog extends JDialog {

    private final JTextPane textPane;
    private final JTextField titleField;
    private final UndoManager undoManager = new UndoManager();
    private final GraphPanel.NodeUI node;
    private final GraphPanel panel;

    public NoteDialog(Window owner, GraphPanel.NodeUI node, GraphPanel panel) {
        super(owner, "✏️ Editor de Nota", ModalityType.APPLICATION_MODAL);
        this.node = node;
        this.panel = panel;
        
        setSize(800, 600);
        setLayout(new BorderLayout(0, 0));
        
        // Background moderno
        getContentPane().setBackground(new Color(248, 249, 250));

        // === HEADER MODERNO ===
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JPanel titlePanel = new JPanel(new BorderLayout(8, 0));
        titlePanel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("📝 Título:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(70, 70, 75));
        
        titleField = new JTextField(node.label);
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(titleField, BorderLayout.CENTER);

        JButton colorBtn = createModernButton("🎨 Cor", node.color);
        colorBtn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(NoteDialog.this, "Escolher cor do nó", node.color);
            if (chosen != null) {
                node.color = chosen;
                node.colorHex = String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
                colorBtn.setBackground(chosen);
                colorBtn.setForeground(getBrightness(chosen) > 128 ? Color.BLACK : Color.WHITE);
            }
        });

        header.add(titlePanel, BorderLayout.CENTER);
        header.add(colorBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // === EDITOR DE TEXTO ===
        textPane = new JTextPane();
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textPane.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        
        try {
            textPane.setContentType("text/html");
            textPane.setText(node.note == null ? "" : node.note);
        } catch (Exception ex) {
            textPane.setText(node.note == null ? "" : node.note);
        }

        textPane.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        // Keybindings para undo/redo
        InputMap im = textPane.getInputMap();
        ActionMap am = textPane.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "editorUndo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "editorRedo");
        am.put("editorUndo", new AbstractAction(){ 
            @Override public void actionPerformed(ActionEvent e) { 
                if (undoManager.canUndo()) undoManager.undo(); 
            }
        });
        am.put("editorRedo", new AbstractAction(){ 
            @Override public void actionPerformed(ActionEvent e) { 
                if (undoManager.canRedo()) undoManager.redo(); 
            }
        });

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // === TOOLBAR DE FORMATAÇÃO ===
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBackground(new Color(248, 249, 250));
        toolbarPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(248, 249, 250));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Botões de formatação
        JButton boldBtn = createToolButton("B", "Negrito", Font.BOLD);
        JButton italicBtn = createToolButton("I", "Itálico", Font.ITALIC);
        JButton underlineBtn = createToolButton("U", "Sublinhado", Font.PLAIN);

        boldBtn.addActionListener(e -> toggleStyle(StyleConstants.CharacterConstants.Bold));
        italicBtn.addActionListener(e -> toggleStyle(StyleConstants.CharacterConstants.Italic));
        underlineBtn.addActionListener(e -> toggleStyle(StyleConstants.CharacterConstants.Underline));

        toolbar.add(boldBtn);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(italicBtn);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(underlineBtn);
        toolbar.addSeparator(new Dimension(16, 32));

        // Tamanho da fonte
        JLabel sizeLabel = new JLabel("Tamanho:");
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sizeLabel.setForeground(new Color(100, 100, 105));
        
        SpinnerNumberModel sizeModel = new SpinnerNumberModel(14, 8, 72, 1);
        JSpinner sizeSpin = new JSpinner(sizeModel);
        sizeSpin.setPreferredSize(new Dimension(60, 28));
        sizeSpin.addChangeListener(e -> setFontSize((int) sizeSpin.getValue()));

        toolbar.add(sizeLabel);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(sizeSpin);
        toolbar.addSeparator(new Dimension(16, 32));

        // Cor do texto
        JButton textColorBtn = createToolButton("🎨", "Cor do Texto", Font.PLAIN);
        textColorBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(NoteDialog.this, "Cor do texto", Color.BLACK);
            if (c != null) applyTextColor(c);
        });
        toolbar.add(textColorBtn);
        toolbar.addSeparator(new Dimension(16, 32));

        // Imagem e lista
        JButton imgBtn = createToolButton("🖼️", "Inserir Imagem", Font.PLAIN);
        JButton listBtn = createToolButton("• Lista", "Adicionar Item", Font.PLAIN);
        
        imgBtn.addActionListener(e -> insertImage());
        listBtn.addActionListener(e -> insertBullet());

        toolbar.add(imgBtn);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(listBtn);

        toolbarPanel.add(toolbar, BorderLayout.WEST);
        add(toolbarPanel, BorderLayout.SOUTH);

        // === RODAPÉ COM BOTÕES ===
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(new Color(248, 249, 250));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton createLinkedBtn = createFooterButton("🔗 Criar Nó Ligado", new Color(155, 89, 182));
        JButton cancelBtn = createFooterButton("✖️ Cancelar", new Color(149, 165, 166));
        JButton saveBtn = createFooterButton("✓ Guardar", new Color(46, 204, 113));

        createLinkedBtn.addActionListener(e -> {
            saveNodeData();
            GraphPanel.NodeUI newNode = new GraphPanel.NodeUI(
                node.x + 100,
                node.y + 60,
                "Nova Nota",
                "",
                node.color,
                node.colorHex
            );
            panel.pushSnapshot();
            panel.addNode(newNode);
            panel.addEdge(node, newNode);
            panel.pushSnapshot();
            dispose();
            panel.openNodeEditor(newNode);
        });

        cancelBtn.addActionListener(e -> dispose());
        
        saveBtn.addActionListener(e -> {
            saveNodeData();
            dispose();
        });

        footer.add(createLinkedBtn);
        footer.add(cancelBtn);
        footer.add(saveBtn);
        add(footer, BorderLayout.PAGE_END);
    }

    private void saveNodeData() {
        node.label = titleField.getText();
        node.note = exportAsHTML(textPane);
    }

    private JButton createModernButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(getBrightness(bgColor) > 128 ? Color.BLACK : Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 36));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(bgColor.brighter());
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }

    private JButton createToolButton(String text, String tooltip, int fontStyle) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", fontStyle, 13));
        btn.setToolTipText(tooltip);
        btn.setBackground(new Color(255, 255, 255));
        btn.setForeground(new Color(70, 70, 75));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(text.length() > 2 ? 80 : 36, 32));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(new Color(240, 242, 245));
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(Color.WHITE);
            }
        });
        
        return btn;
    }

    private JButton createFooterButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(150, 38));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(bgColor.brighter());
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }

    private int getBrightness(Color c) {
        return (int) Math.sqrt(
            c.getRed() * c.getRed() * .241 +
            c.getGreen() * c.getGreen() * .691 +
            c.getBlue() * c.getBlue() * .068
        );
    }

    // -------------------------
    // Formatação de texto
    // -------------------------
    private void toggleStyle(Object style) {
        StyledDocument doc = textPane.getStyledDocument();
        int a = textPane.getSelectionStart(), b = textPane.getSelectionEnd();
        if (a == b) return;
        Element el = doc.getCharacterElement(a);
        AttributeSet as = el.getAttributes();
        SimpleAttributeSet sas = new SimpleAttributeSet();
        if (style == StyleConstants.CharacterConstants.Bold) 
            StyleConstants.setBold(sas, !StyleConstants.isBold(as));
        else if (style == StyleConstants.CharacterConstants.Italic) 
            StyleConstants.setItalic(sas, !StyleConstants.isItalic(as));
        else if (style == StyleConstants.CharacterConstants.Underline) 
            StyleConstants.setUnderline(sas, !StyleConstants.isUnderline(as));
        doc.setCharacterAttributes(a, b-a, sas, false);
    }

    private void setFontSize(int size) {
        StyledDocument doc = textPane.getStyledDocument();
        int a = textPane.getSelectionStart(), b = textPane.getSelectionEnd();
        if (a == b) return;
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setFontSize(sas, size);
        doc.setCharacterAttributes(a, b-a, sas, false);
    }

    private void applyTextColor(Color c) {
        StyledDocument doc = textPane.getStyledDocument();
        int a = textPane.getSelectionStart(), b = textPane.getSelectionEnd();
        if (a == b) return;
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setForeground(sas, c);
        doc.setCharacterAttributes(a, b-a, sas, false);
    }

    private void insertBullet() {
        try { 
            textPane.getDocument().insertString(textPane.getCaretPosition(), "\n• ", null); 
        } catch (BadLocationException ex) { 
            ex.printStackTrace(); 
        }
    }

    private void insertImage() {
        try {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imagens", "png","jpg","jpeg","gif","bmp"
            ));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File f = fc.getSelectedFile();
            BufferedImage bi = ImageIO.read(f);
            
            // Redimensionar se muito grande
            int maxWidth = 400;
            if (bi.getWidth() > maxWidth) {
                int newHeight = (int) (bi.getHeight() * ((double) maxWidth / bi.getWidth()));
                Image scaled = bi.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);
                textPane.insertIcon(icon);
            } else {
                ImageIcon icon = new ImageIcon(bi);
                textPane.insertIcon(icon);
            }
        } catch (Exception ex) { 
            JOptionPane.showMessageDialog(this, 
                "Erro ao inserir imagem: " + ex.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE
            ); 
        }
    }

    private String exportAsHTML(JTextPane pane) {
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            java.io.StringWriter writer = new java.io.StringWriter();
            kit.write(writer, pane.getDocument(), 0, pane.getDocument().getLength());
            return writer.toString();
        } catch (Exception ex) {
            return pane.getText();
        }
    }
}