package jotes.ui.canvas;

import jotes.db.CanvasRepository;
import jotes.db.NoteRepository;
import jotes.model.CanvasBoard;
import jotes.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Janela do canvas: barra com seletor de quadros e ações, e o {@link CanvasPanel} no centro.
 * Duplo clique num cartão de nota abre a nota na janela principal.
 */
public class CanvasWindow extends JFrame {

    private final CanvasRepository canvasRepo;
    private final CanvasPanel panel;
    private final JComboBox<CanvasBoard> boardCombo = new JComboBox<>();
    private final JLabel zoomLabel = new JLabel("100%");
    private boolean updatingCombo;
    private Consumer<Long> onOpenNote = id -> {};

    public CanvasWindow(CanvasRepository canvasRepo, NoteRepository noteRepo) {
        super("Canvas");
        this.canvasRepo = canvasRepo;
        panel = new CanvasPanel(canvasRepo, noteRepo);
        panel.setOnOpenNote(id -> onOpenNote.accept(id));

        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);

        add(buildToolbar(), BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

        JLabel hint = new JLabel("Duplo clique: novo cartão · porta à direita: ligar · duplo clique na ligação: etiqueta · "
                + "Shift+arrastar: selecionar · Ctrl+D: duplicar · canto inferior: redimensionar · Ctrl+roda: zoom",
                SwingConstants.CENTER);
        hint.setForeground(Theme.dimForeground());
        hint.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        add(hint, BorderLayout.SOUTH);

        // atualiza títulos/excertos das notas sempre que a janela ganha foco
        addWindowFocusListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent e) {
                panel.refreshNotes();
            }
        });

        panel.setOnZoomChanged(() -> zoomLabel.setText(Math.round(panel.getZoom() * 100) + "%"));

        loadBoards();
    }

    /** Regista o callback chamado com o id da nota quando o utilizador a abre a partir do canvas. */
    public void setOnOpenNote(Consumer<Long> listener) {
        this.onOpenNote = listener;
    }

    private JToolBar buildToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        boardCombo.setPrototypeDisplayValue(prototype());
        boardCombo.addActionListener(e -> {
            if (updatingCombo) return;
            CanvasBoard b = (CanvasBoard) boardCombo.getSelectedItem();
            if (b != null) panel.loadBoard(b.getId());
        });

        JButton newBoard = new JButton("Novo");
        newBoard.addActionListener(e -> createBoard());
        JButton rename = new JButton("Renomear");
        rename.addActionListener(e -> renameBoard());
        JButton delete = new JButton("Apagar");
        delete.addActionListener(e -> deleteBoard());

        JButton addCard = new JButton("+ Cartão");
        addCard.addActionListener(e -> panel.addCardAtCenter());
        JButton addNote = new JButton("+ Nota");
        addNote.addActionListener(e -> panel.addNoteAtCenter(this));
        JButton addImage = new JButton("+ Imagem");
        addImage.addActionListener(e -> panel.addImageAtCenter(this));

        JButton zoomOut = new JButton("−");
        zoomOut.addActionListener(e -> panel.zoomOut());
        JButton zoomIn = new JButton("+");
        zoomIn.addActionListener(e -> panel.zoomIn());
        JButton fit = new JButton("Ajustar");
        fit.addActionListener(e -> panel.resetView());

        JButton export = new JButton("Exportar…");
        export.addActionListener(e -> exportBoard());

        bar.add(new JLabel("Quadro: "));
        bar.add(boardCombo);
        bar.add(newBoard);
        bar.add(rename);
        bar.add(delete);
        bar.addSeparator();
        bar.add(addCard);
        bar.add(addNote);
        bar.add(addImage);
        bar.addSeparator();
        bar.add(zoomOut);
        bar.add(zoomLabel);
        bar.add(zoomIn);
        bar.add(fit);
        bar.addSeparator();
        bar.add(export);

        zoomLabel.setForeground(Theme.TEXT_DIM);
        zoomLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        for (Component c : bar.getComponents()) {
            if (c instanceof AbstractButton b) Theme.styleToolbarButton(b);
        }
        return bar;
    }

    /** Exporta o quadro atual como imagem PNG ou SVG. */
    private void exportBoard() {
        CanvasBoard board = (CanvasBoard) boardCombo.getSelectedItem();
        String base = board == null ? "canvas" : board.getName().replaceAll("[^\\p{L}\\p{N}]+", "-");

        String[] formats = {"PNG (imagem)", "SVG (vetorial)"};
        Object choice = JOptionPane.showInputDialog(this, "Formato:", "Exportar canvas",
                JOptionPane.PLAIN_MESSAGE, null, formats, formats[0]);
        if (choice == null) return;
        boolean png = choice.equals(formats[0]);
        String extension = png ? "png" : "svg";

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar canvas");
        fc.setSelectedFile(new java.io.File(base + "." + extension));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.nio.file.Path target = fc.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase().endsWith("." + extension)) {
            target = target.resolveSibling(target.getFileName() + "." + extension);
        }
        try {
            boolean written = png ? CanvasExporter.exportPng(panel, target)
                    : CanvasExporter.exportSvg(panel, target);
            JOptionPane.showMessageDialog(this,
                    written ? "Guardado em:\n" + target : "O quadro está vazio — nada para exportar.",
                    "Exportar canvas",
                    written ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            jotes.util.Log.warn(CanvasWindow.class, "Falha ao exportar o canvas", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBoards() {
        try {
            updatingCombo = true;
            try {
                boardCombo.removeAllItems();
                for (CanvasBoard b : canvasRepo.listBoards()) boardCombo.addItem(b);
                if (boardCombo.getItemCount() == 0) {
                    boardCombo.addItem(canvasRepo.createBoard("Canvas 1"));
                }
            } finally {
                updatingCombo = false;
            }
            boardCombo.setSelectedIndex(0);
            CanvasBoard b = (CanvasBoard) boardCombo.getSelectedItem();
            if (b != null) panel.loadBoard(b.getId());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void createBoard() {
        String name = JOptionPane.showInputDialog(this, "Nome do quadro:", "Novo canvas",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        try {
            CanvasBoard b = canvasRepo.createBoard(name.trim());
            updatingCombo = true;
            boardCombo.addItem(b);
            updatingCombo = false;
            boardCombo.setSelectedItem(b);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void renameBoard() {
        CanvasBoard b = (CanvasBoard) boardCombo.getSelectedItem();
        if (b == null) return;
        String name = JOptionPane.showInputDialog(this, "Nome do quadro:", b.getName());
        if (name == null || name.isBlank()) return;
        try {
            canvasRepo.renameBoard(b.getId(), name.trim());
            b.setName(name.trim());
            boardCombo.repaint();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteBoard() {
        CanvasBoard b = (CanvasBoard) boardCombo.getSelectedItem();
        if (b == null) return;
        int r = JOptionPane.showConfirmDialog(this,
                "Apagar o quadro \"" + b.getName() + "\" e todos os seus cartões?",
                "Apagar canvas", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            canvasRepo.deleteBoard(b.getId());
            loadBoards();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private static CanvasBoard prototype() {
        CanvasBoard b = new CanvasBoard();
        b.setName("Quadro de exemplo — nome longo");
        return b;
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
