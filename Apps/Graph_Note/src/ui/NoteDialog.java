package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog to edit node note and title, and to create a connected node quickly.
 */
public class NoteDialog extends JDialog {
    public NoteDialog(Window owner, GraphPanel.NodeUI node, GraphPanel panel) {
        super(owner, "Nota: " + (node.label==null?"":node.label), ModalityType.APPLICATION_MODAL);
        setSize(420, 320);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        JTextField title = new JTextField(node.label);
        top.add(new JLabel("Título:"), BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JTextArea area = new JTextArea(node.note);
        add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton newNodeBtn = new JButton("Criar Nó Ligado");
        JButton saveBtn = new JButton("Salvar");
        JButton cancelBtn = new JButton("Cancelar");
        bottom.add(newNodeBtn);
        bottom.add(saveBtn);
        bottom.add(cancelBtn);
        add(bottom, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            node.label = title.getText();
            node.note = area.getText();
            dispose();
        });

        cancelBtn.addActionListener(e -> dispose());

        newNodeBtn.addActionListener(e -> {
            GraphPanel.NodeUI other = new GraphPanel.NodeUI(node.x + 80, node.y + 20, "Nova nota", "");
            panel.addNode(other);
            panel.addEdge(node, other);
            dispose();
            panel.openNodeEditor(other);
        });
    }
}
