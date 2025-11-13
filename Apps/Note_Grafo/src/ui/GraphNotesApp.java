package ui;

import ui.GraphPanel;

import javax.swing.*;
import java.awt.*;

public class GraphNotesApp {
	public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Graph Notes");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1300, 900);

            GraphPanel panel = new GraphPanel();
            frame.add(panel, BorderLayout.CENTER);

            // === MENU BAR ===
            JMenuBar menuBar = new JMenuBar();

            // File Menu
            JMenu fileMenu = new JMenu("File");
            JMenuItem saveItem = new JMenuItem("Save");
            JMenuItem saveAsItem = new JMenuItem("Save As");
            JMenuItem loadItem = new JMenuItem("Load");
            JMenuItem mergeItem = new JMenuItem("Merge Graph");
            JMenuItem clearItem = new JMenuItem("Clear Graph");

            saveItem.addActionListener(e -> panel.saveToFile(frame));
            saveAsItem.addActionListener(e -> panel.saveAs(frame));
            loadItem.addActionListener(e -> panel.loadFromFile(frame));
            mergeItem.addActionListener(e -> panel.mergeFromFile(frame));
            clearItem.addActionListener(e -> panel.clearGraph());

            fileMenu.add(saveItem);
            fileMenu.add(saveAsItem);
            fileMenu.add(loadItem);
            fileMenu.add(mergeItem);
            fileMenu.addSeparator();
            fileMenu.add(clearItem);

            // Image Menu
            JMenu imageMenu = new JMenu("Image");
            JMenuItem exportPngItem = new JMenuItem("Export PNG");
            JMenuItem exportJpgItem = new JMenuItem("Export JPG");

            exportPngItem.addActionListener(e -> panel.exportImage(frame, "png"));
            exportJpgItem.addActionListener(e -> panel.exportImage(frame, "jpg"));

            imageMenu.add(exportPngItem);
            imageMenu.add(exportJpgItem);

            // Window Menu
            JMenu windowMenu = new JMenu("Window");
            JMenuItem toggleLayoutItem = new JMenuItem("Toggle Layout");
            JMenuItem undoItem = new JMenuItem("Undo");
            JMenuItem redoItem = new JMenuItem("Redo");
            JMenuItem resetZoomItem = new JMenuItem("Reset Zoom");
            JMenuItem themeItem = new JMenuItem("Toggle Theme");

            toggleLayoutItem.addActionListener(e -> panel.toggleLayoutRunning());
            undoItem.addActionListener(e -> panel.undo());
            redoItem.addActionListener(e -> panel.redo());
            resetZoomItem.addActionListener(e -> panel.resetZoomAndPan());
            themeItem.addActionListener(e -> { panel.setDarkMode(!panel.isDarkMode()); panel.repaint(); });

            windowMenu.add(toggleLayoutItem);
            windowMenu.add(undoItem);
            windowMenu.add(redoItem);
            windowMenu.add(resetZoomItem);
            windowMenu.addSeparator();
            windowMenu.add(themeItem);

            menuBar.add(fileMenu);
            menuBar.add(imageMenu);
            menuBar.add(windowMenu);
            frame.setJMenuBar(menuBar);

            // === TOOLBAR (Topo - simplificado) ===
            JToolBar toolbar = new JToolBar();
            toolbar.setFloatable(false);

            JButton addBtn = new JButton("Add Node");
            JButton linkBtn = new JButton("Link Nodes");

            addBtn.addActionListener(e -> panel.startAddingNodeMode());
            linkBtn.addActionListener(e -> panel.startLinkingMode());

            toolbar.add(addBtn);
            toolbar.add(linkBtn);

            frame.add(toolbar, BorderLayout.NORTH);

            // === BOTTOM BAR (Rodapé) ===
            JPanel bottomBar = new JPanel(new BorderLayout());
            bottomBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            // Lado direito - Zoom Slider
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            rightPanel.add(new JLabel("Zoom: "));
            JSlider zoomSlider = new JSlider(10, 400, 100);
            zoomSlider.setPreferredSize(new Dimension(150, 24));
            zoomSlider.addChangeListener(e -> {
                double v = zoomSlider.getValue() / 100.0;
                panel.scale = v;
                panel.repaint();
            });
            rightPanel.add(zoomSlider);

            bottomBar.add(rightPanel, BorderLayout.EAST);

            frame.add(bottomBar, BorderLayout.SOUTH);

            frame.setVisible(true);
        });
    }
}
