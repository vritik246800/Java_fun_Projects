package ui;

import dao.ProdutoDAO;
import dao.StockMovimentoDAO;
import model.Produto;
import enums.TipoMovimentoStock;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class EntradaStockUI extends JFrame {

    private JComboBox<Produto> cbProduto;
    private JTextField txtQuantidade;

    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private StockMovimentoDAO movDAO = new StockMovimentoDAO();

    public EntradaStockUI() {
        setTitle("Entrada de Stock (Fornecedor)");
        setSize(350, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
    }

    private void initUI() {
        setLayout(new GridLayout(3,2,5,5));

        try {
            cbProduto = new JComboBox<>(produtoDAO.listarTodos().toArray(new Produto[0]));
        } catch (Exception e) {
            cbProduto = new JComboBox<>();
        }

        add(new JLabel("Produto:"));
        add(cbProduto);

        add(new JLabel("Quantidade:"));
        txtQuantidade = new JTextField();
        add(txtQuantidade);

        JButton salvar = new JButton("Registrar Entrada");
        salvar.addActionListener(e -> registrarEntrada());
        add(salvar);
    }

    private void registrarEntrada() {
        try {
            Produto p = (Produto) cbProduto.getSelectedItem();
            BigDecimal qtd = new BigDecimal(txtQuantidade.getText());

            BigDecimal stockAntes = p.getStockAtual();
            BigDecimal stockDepois = stockAntes.add(qtd);

            produtoDAO.atualizarStock(p.getId(), stockDepois);

            // movimento
            movDAO.registrarMovimento(p, qtd, stockAntes, stockDepois, TipoMovimentoStock.ENTRADA, "Compra ao fornecedor");

            JOptionPane.showMessageDialog(this, "Entrada registrada!");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }
}
