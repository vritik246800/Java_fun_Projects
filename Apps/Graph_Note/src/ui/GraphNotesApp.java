import ui.GraphPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Main launcher (default package).
 */
public class GraphNotesApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Graph Notes - Gson version");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000,700);

            GraphPanel panel = new GraphPanel();
            frame.add(panel, BorderLayout.CENTER);

            JToolBar tb = new JToolBar();
            JButton addBtn = new JButton("Add Node");
            JButton linkBtn = new JButton("Link Nodes");
            JButton saveBtn = new JButton("Save");
            JButton loadBtn = new JButton("Load");
            JButton clearBtn = new JButton("Clear");
            JButton toggleBtn = new JButton("Toggle Layout");

            tb.add(addBtn); tb.add(linkBtn); tb.addSeparator();
            tb.add(saveBtn); tb.add(loadBtn); tb.add(clearBtn); tb.addSeparator();
            tb.add(toggleBtn);

            frame.add(tb, BorderLayout.NORTH);

            addBtn.addActionListener(e -> panel.startAddingNodeMode());
            linkBtn.addActionListener(e -> panel.startLinkingMode());
            saveBtn.addActionListener(e -> panel.saveToFile(frame));
            loadBtn.addActionListener(e -> panel.loadFromFile(frame));
            clearBtn.addActionListener(e -> panel.clearGraph());
            toggleBtn.addActionListener(e -> panel.toggleLayoutRunning());

            frame.setVisible(true);
        });
    }
}
