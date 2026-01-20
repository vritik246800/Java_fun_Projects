package ui;

import java.awt.*;
import javax.swing.*;

public class PedidoTransferenciaDialog extends JDialog {

    public PedidoTransferenciaDialog(JFrame parent, String ficheiro, Runnable onAccept, Runnable onReject) {
        super(parent, "Pedido de Transferência", true);

        JLabel label = new JLabel("Deseja aceitar o ficheiro:");
        JLabel ficheiroLabel = new JLabel(ficheiro);

        JButton aceitarBtn = new JButton("Aceitar");
        JButton rejeitarBtn = new JButton("Rejeitar");

        aceitarBtn.addActionListener(e -> {
            onAccept.run();
            dispose();
        });

        rejeitarBtn.addActionListener(e -> {
            onReject.run();
            dispose();
        });

        JPanel center = new JPanel(new GridLayout(2, 1));
        center.add(label);
        center.add(ficheiroLabel);

        JPanel buttons = new JPanel();
        buttons.add(aceitarBtn);
        buttons.add(rejeitarBtn);

        setLayout(new BorderLayout(10, 10));
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        setSize(350, 150);
        setLocationRelativeTo(parent);
    }
}
