package ui;

import dao.ProdutoDAO;
import model.Produto;
import enums.CategoriaProduto;
import enums.TipoVenda;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class ProdutoListaUI extends JFrame {

    private JTable tabela;
    private DefaultTableModel modelo;
    private ProdutoDAO dao = new ProdutoDAO();

    public ProdutoListaUI() {
        setTitle("Lista de Produtos");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        carregarTabela();
    }

    private void initUI() {
        modelo = new DefaultTableModel(
                new String[]{"ID", "Código", "Nome", "Categoria", "Tipo", "Preço", "IVA", "Stock"}, 0);

        tabela = new JTable(modelo);
        JScrollPane scroll = new JScrollPane(tabela);

        JButton btnEditar = new JButton("Editar Produto");
        btnEditar.addActionListener(e -> editarProduto());

        JButton btnRefresh = new JButton("Recarregar");
        btnRefresh.addActionListener(e -> carregarTabela());

        JPanel botoes = new JPanel();
        botoes.add(btnEditar);
        botoes.add(btnRefresh);

        add(scroll, BorderLayout.CENTER);
        add(botoes, BorderLayout.SOUTH);
    }

    private void carregarTabela() {
        try {
            modelo.setRowCount(0);

            List<Produto> produtos = dao.listarTodos();

            for (Produto p : produtos) {
                modelo.addRow(new Object[]{
                        p.getId(),
                        p.getCodigo(),
                        p.getNome(),
                        p.getCategoria(),
                        p.getTipoVenda(),
                        p.getPrecoBase(),
                        p.isTemIva() ? p.getTaxaIva() : "0",
                        p.getStockAtual()
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar produtos");
        }
    }

    private void editarProduto() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um produto");
            return;
        }

        int id = (int) tabela.getValueAt(linha, 0);

        new ProdutoEditarUI(id).setVisible(true);
    }
}
