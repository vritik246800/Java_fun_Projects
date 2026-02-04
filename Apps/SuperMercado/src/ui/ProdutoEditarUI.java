package ui;

import dao.ProdutoDAO;
import enums.CategoriaProduto;
import enums.TipoVenda;
import model.Produto;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class ProdutoEditarUI extends JFrame {

    private ProdutoDAO dao = new ProdutoDAO();
    private Produto produto;

    private JTextField txtNome, txtPreco, txtIva, txtStock;
    private JComboBox<CategoriaProduto> cbCat;
    private JComboBox<TipoVenda> cbTipo;
    private JCheckBox chkIva;

    public ProdutoEditarUI(int idProduto) {

        setTitle("Editar Produto");
        setSize(350, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        carregarProduto(idProduto);
        initUI();
    }

    private void carregarProduto(int id) {
        try {
            produto = dao.buscarPorId(id);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar produto.");
        }
    }

    private void initUI() {

        setLayout(new GridLayout(8,2));

        add(new JLabel("Nome:"));
        txtNome = new JTextField(produto.getNome());
        add(txtNome);

        add(new JLabel("Categoria:"));
        cbCat = new JComboBox<>(CategoriaProduto.values());
        cbCat.setSelectedItem(produto.getCategoria());
        add(cbCat);

        add(new JLabel("Tipo Venda:"));
        cbTipo = new JComboBox<>(TipoVenda.values());
        cbTipo.setSelectedItem(produto.getTipoVenda());
        add(cbTipo);

        add(new JLabel("Preço Base:"));
        txtPreco = new JTextField(produto.getPrecoBase().toString());
        add(txtPreco);

        add(new JLabel("Tem IVA:"));
        chkIva = new JCheckBox("", produto.isTemIva());
        add(chkIva);

        add(new JLabel("IVA (%):"));
        txtIva = new JTextField(produto.getTaxaIva().toString());
        txtIva.setEnabled(produto.isTemIva());
        add(txtIva);

        chkIva.addActionListener(e -> txtIva.setEnabled(chkIva.isSelected()));

        add(new JLabel("Stock:"));
        txtStock = new JTextField(produto.getStockAtual().toString());
        add(txtStock);

        JButton salvar = new JButton("Salvar");
        salvar.addActionListener(e -> salvarProduto());
        add(salvar);

        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        add(cancelar);
    }

    private void salvarProduto() {
        try {
            produto.setNome(txtNome.getText().trim());
            produto.setCategoria((CategoriaProduto) cbCat.getSelectedItem());
            produto.setTipoVenda((TipoVenda) cbTipo.getSelectedItem());
            produto.setPrecoBase(new BigDecimal(txtPreco.getText()));
            produto.setTemIva(chkIva.isSelected());

            if (chkIva.isSelected())
                produto.setTaxaIva(new BigDecimal(txtIva.getText()));
            else
                produto.setTaxaIva(BigDecimal.ZERO);

            produto.setStockAtual(new BigDecimal(txtStock.getText()));

            dao.atualizarProduto(produto);

            JOptionPane.showMessageDialog(this, "Produto atualizado!");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + ex.getMessage());
        }
    }
}
