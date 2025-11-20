package ui;

import modelo.Produto;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PainelProdutos extends JPanel {

    private List<Produto> produtos;

    public PainelProdutos(List<Produto> produtos) {
        this.produtos = produtos;
        setLayout(new BorderLayout());

        add(criarFormulario(), BorderLayout.NORTH);
        add(criarTabela(), BorderLayout.CENTER);
    }

    private JPanel criarFormulario() {
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Novo Produto"));

        JTextField txtNome = new JTextField();
        JTextField txtCusto = new JTextField("0.0");
        JTextField txtMargem = new JTextField("0.0");
        JCheckBox chkIVA = new JCheckBox("Incluir IVA (16%)");
        JTextField txtStock = new JTextField("0");
        JLabel lblPrecoFinal = new JLabel("Preço Venda: 0.00 MT");

        JButton btnSalvar = new JButton("Salvar Produto");

        // Cálculo automático
        DocumentListener calc = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calcular(); }
            public void removeUpdate(DocumentEvent e) { calcular(); }
            public void changedUpdate(DocumentEvent e) { calcular(); }
            void calcular() {
                try {
                    double custo = Double.parseDouble(txtCusto.getText());
                    double margem = Double.parseDouble(txtMargem.getText());
                    double preco = custo + (custo * margem / 100);

                    if (chkIVA.isSelected()) preco *= 1.16;

                    lblPrecoFinal.setText(String.format("Preço Venda: %.2f MT", preco));
                } catch (Exception ignored) {}
            }
        };

        txtCusto.getDocument().addDocumentListener(calc);
        txtMargem.getDocument().addDocumentListener(calc);
        chkIVA.addActionListener(e -> calc.changedUpdate(null));

        btnSalvar.addActionListener(e -> {
            try {
                Produto p = new Produto(
                        txtNome.getText(),
                        Double.parseDouble(txtCusto.getText()),
                        Double.parseDouble(txtMargem.getText()),
                        chkIVA.isSelected(),
                        Integer.parseInt(txtStock.getText())
                );
                produtos.add(p);
                JOptionPane.showMessageDialog(this, "Produto salvo!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro nos valores!");
            }
        });

        formPanel.add(new JLabel("Nome:")); formPanel.add(txtNome);
        formPanel.add(new JLabel("Custo:")); formPanel.add(txtCusto);
        formPanel.add(new JLabel("Margem %:")); formPanel.add(txtMargem);
        formPanel.add(new JLabel("Stock:")); formPanel.add(txtStock);
        formPanel.add(chkIVA); formPanel.add(lblPrecoFinal);
        formPanel.add(new JLabel("")); formPanel.add(btnSalvar);

        return formPanel;
    }

    private JScrollPane criarTabela() {
        String[] colunas = {"Nome", "Custo", "Margem", "Venda", "Stock"};
        DefaultTableModel model = new DefaultTableModel(colunas, 0);
        JTable tabela = new JTable(model);

        JButton btnRefresh = new JButton("Atualizar Lista");
        btnRefresh.addActionListener(e -> {
            model.setRowCount(0);
            for (Produto p : produtos) {
                model.addRow(new Object[]{
                        p.getNome(),
                        p.getPrecoCusto(),
                        p.getMargemLucro(),
                        p.getPrecoFinal(),
                        p.getStock()
                });
            }
        });

        JPanel south = new JPanel();
        south.add(btnRefresh);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JScrollPane(tabela), BorderLayout.CENTER);
        wrapper.add(south, BorderLayout.SOUTH);

        return new JScrollPane(wrapper);
    }
}
