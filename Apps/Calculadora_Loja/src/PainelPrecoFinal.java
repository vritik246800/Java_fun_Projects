
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public class PainelPrecoFinal extends JPanel {

    private JTextField txtBase;
    private JSpinner spIVA, spMargem;
    private JLabel lblFinal;

    public PainelPrecoFinal() {
        setLayout(null);

        txtBase = new JTextField();
        txtBase.setBounds(40, 80, 140, 30);

        spIVA = new JSpinner(new SpinnerNumberModel(16.0, 0, 100, 0.5));
        spIVA.setBounds(220, 80, 120, 30);

        spMargem = new JSpinner(new SpinnerNumberModel(15.0, 0, 300, 1));
        spMargem.setBounds(40, 140, 140, 30);

        lblFinal = new JLabel("0.00");
        lblFinal.setBounds(220, 140, 160, 40);
        lblFinal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblFinal.setForeground(new Color(0, 120, 215));

        add(txtBase);
        add(spIVA);
        add(spMargem);
        add(lblFinal);

        DocumentListener d = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calc(); }
            public void removeUpdate(DocumentEvent e) { calc(); }
            public void changedUpdate(DocumentEvent e) { calc(); }
        };

        txtBase.getDocument().addDocumentListener(d);
        spIVA.addChangeListener(e -> calc());
        spMargem.addChangeListener(e -> calc());
    }

    private void calc() {
        try {
            double base = Double.parseDouble(txtBase.getText());
            double iva = (double) spIVA.getValue();
            double margem = (double) spMargem.getValue();

            double r = base * (1 + margem / 100);
            r = r * (1 + iva / 100);

            lblFinal.setText(String.format("%.2f", r));
        } catch (Exception e) {
            lblFinal.setText("0.00");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(245, 247, 250));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(20, 20, 380, 230, 20, 20);

        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString("Preço Final", 40, 55);
    }
}
