package ui;

import dao.ProdutoDAO;
import enums.CategoriaProduto;
import enums.TipoVenda;
import model.Produto;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class ProdutoCadastroUI extends JFrame {

    private JTextField txtCodigo, txtNome, txtPreco, txtIva, txtStock;
    private JComboBox<CategoriaProduto> cbCategoria;
    private JComboBox<TipoVenda> cbTipoVenda;
    private JCheckBox chkTemIva;

    private ProdutoDAO dao = new ProdutoDAO();

    public ProdutoCadastroUI() {
        setTitle("Cadastro de Produto");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
    }

    private void initUI() {
        setLayout(new GridLayout(10, 2, 5, 5));

        add(new JLabel("Código:"));
        txtCodigo = new JTextField();
        add(txtCodigo);

        add(new JLabel("Nome:"));
        txtNome = new JTextField();
        add(txtNome);

        add(new JLabel("Categoria:"));
        cbCategoria = new JComboBox<>(CategoriaProduto.values());
        add(cbCategoria);

        add(new JLabel("Tipo Venda:"));
        cbTipoVenda = new JComboBox<>(TipoVenda.values());
        add(cbTipoVenda);

        add(new JLabel("Preço Base:"));
        txtPreco = new JTextField();
        add(txtPreco);

        add(new JLabel("Tem IVA?"));
        chkTemIva = new JCheckBox();
        chkTemIva.addActionListener(e -> txtIva.setEnabled(chkTemIva.isSelected()));
        add(chkTemIva);

        add(new JLabel("Taxa IVA (%):"));
        txtIva = new JTextField();
        txtIva.setEnabled(false);
        add(txtIva);

        add(new JLabel("Stock Inicial:"));
        txtStock = new JTextField();
        add(txtStock);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvarProduto());
        add(btnSalvar);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        add(btnCancelar);
    }

    private void salvarProduto() {
        try {
            Produto p = new Produto();

            p.setCodigo(txtCodigo.getText().trim());
            p.setNome(txtNome.getText().trim());
            p.setCategoria((CategoriaProduto) cbCategoria.getSelectedItem());
            p.setTipoVenda((TipoVenda) cbTipoVenda.getSelectedItem());
            p.setPrecoBase(new BigDecimal(txtPreco.getText().trim()));

            p.setTemIva(chkTemIva.isSelected());
            if (chkTemIva.isSelected()) {
                p.setTaxaIva(new BigDecimal(txtIva.getText().trim()));
            } else {
                p.setTaxaIva(BigDecimal.ZERO);
            }

            p.setStockAtual(new BigDecimal(txtStock.getText().trim()));
            p.setAtivo(true);

            dao.inserirProduto(p);

            JOptionPane.showMessageDialog(this, "Produto registado com sucesso!");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao gravar produto: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new ProdutoCadastroUI().setVisible(true);
    }
}
