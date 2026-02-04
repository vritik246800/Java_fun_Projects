package ui;

import dao.ProdutoDAO;
import model.Produto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StockConsultaUI extends JFrame {

    private JTable tabela;
    private DefaultTableModel modelo;
    private ProdutoDAO dao = new ProdutoDAO();

    public StockConsultaUI() {
        setTitle("Consulta de Stock");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        carregarTabela();
    }

    private void initUI() {
        modelo = new DefaultTableModel(
                new String[]{"ID", "Código", "Produto", "Stock", "Categoria", "Tipo", "IVA"}, 0);

        tabela = new JTable(modelo);

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton refresh = new JButton("Atualizar");
        refresh.addActionListener(e -> carregarTabela());

        add(refresh, BorderLayout.SOUTH);
    }

    private void carregarTabela() {
        try {
            modelo.setRowCount(0);

            List<Produto> lista = dao.listarTodos();

            for (Produto p : lista) {
                modelo.addRow(new Object[]{
                        p.getId(),
                        p.getCodigo(),
                        p.getNome(),
                        p.getStockAtual(),
                        p.getCategoria(),
                        p.getTipoVenda(),
                        p.isTemIva() ? p.getTaxaIva() : "0"
                });
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar stock!");
        }
    }
}
