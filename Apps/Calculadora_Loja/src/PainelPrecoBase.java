

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;

public class PainelPrecoBase extends JPanel {

    private JTextField txtPreco;
    private JSpinner spQtd, spIVA;

    private JLabel lblBaseUnit;
    private JLabel lblTotalBase;
    private JLabel lblIVAInfo;

    public PainelPrecoBase() {
        setLayout(new GridLayout(0, 1, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        setBackground(Color.white);

        add(titulo("Cálculo de Preço Base (sem IVA)"));

        txtPreco = campo();
        add(linha("Preço Unitário (com IVA)", txtPreco));

        spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        add(linha("Quantidade", spQtd));

        spIVA = new JSpinner(new SpinnerNumberModel(16.0, 0, 100, 0.5));
        add(linha("IVA (%)", spIVA));

        lblBaseUnit = resultado();
        lblTotalBase = resultado();
        lblIVAInfo = resultadoInfo();

        add(lblBaseUnit);
        add(lblTotalBase);
        add(lblIVAInfo);

        //txtPreco.addActionListener(e -> calcular());
        txtPreco.getDocument().addDocumentListener(new DocumentListener() {
        	
        	public void insertUpdate(DocumentEvent e) {
                calcular();
            }
        	
            public void removeUpdate(DocumentEvent e) {
                calcular();
            }
            
            public void changedUpdate(DocumentEvent e) {
                calcular();
            }
        });

        spQtd.addChangeListener(e -> calcular());
        spIVA.addChangeListener(e -> calcular());
    }

    private void calcular() {
        try {
            double precoComIVA = Double.parseDouble(txtPreco.getText());
            int qtd = (int) spQtd.getValue();
            double iva = (double) spIVA.getValue();

            double baseUnit = precoComIVA / (1 + iva / 100);
            double totalBase = baseUnit / qtd;

            lblBaseUnit.setText(
                    "Preço base unitário: " + String.format("%.2f", baseUnit)
            );

            lblTotalBase.setText(
                    "Total sem IVA: " + String.format("%.2f", totalBase)
            );

            lblIVAInfo.setText(
                    "IVA retirado: " + iva + "%"
            );

        } catch (Exception e) {
            lblBaseUnit.setText("Preço base unitário: --");
            lblTotalBase.setText("Total sem IVA: --");
            lblIVAInfo.setText("IVA retirado: --");
        }
    }

    // ================= UI HELPERS =================

    private JLabel titulo(String t) {
        JLabel l = new JLabel(t, SwingConstants.CENTER);
        l.setForeground(Color.BLACK);
        l.setFont(new Font("Segoe UI", Font.BOLD, 20));
        return l;
    }

    private JTextField campo() {
        JTextField t = new JTextField("0.00");
        t.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        return t;
    }

    private JLabel resultado() {
        JLabel l = new JLabel(" ");
        l.setForeground(Color.RED);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return l;
    }

    private JLabel resultadoInfo() {
        JLabel l = new JLabel(" ");
        l.setForeground(Color.RED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return l;
    }

    private JPanel linha(String t, JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(t);
        l.setForeground(Color.black);
        p.add(l, BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        return p;
    }
}
