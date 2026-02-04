package ui;

import dao.ProdutoDAO;
import dao.VendaDAO;
import model.*;
import enums.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CaixaPOS extends JFrame {

    private JTextField txtCodigo;
    private JTable tabela;
    private DefaultTableModel modelo;
    private JLabel lblTotalBruto, lblTotalIva, lblTotalLiquido;
    private JComboBox<MetodoPagamento> cbPagamento;

    private Venda vendaAtual = new Venda();
    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private VendaDAO vendaDAO = new VendaDAO();

    public CaixaPOS() {
        setTitle("Caixa Registadora");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        criarUI();
    }

    private void criarUI() {
        setLayout(new BorderLayout());

        // Topo
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Código Produto:"));
        txtCodigo = new JTextField(15);
        topo.add(txtCodigo);

        JButton btnAdd = new JButton("Adicionar");
        btnAdd.addActionListener(e -> adicionarProduto());
        topo.add(btnAdd);

        add(topo, BorderLayout.NORTH);

        // Tabela
        modelo = new DefaultTableModel(new String[]{"Produto", "Qtd", "Preço", "IVA", "Total"}, 0);
        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        // Painel Totais
        JPanel totais = new JPanel(new GridLayout(4, 2));
        totais.add(new JLabel("Total Bruto:"));
        lblTotalBruto = new JLabel("0.00");
        totais.add(lblTotalBruto);

        totais.add(new JLabel("Total IVA:"));
        lblTotalIva = new JLabel("0.00");
        totais.add(lblTotalIva);

        totais.add(new JLabel("Total Líquido:"));
        lblTotalLiquido = new JLabel("0.00");
        totais.add(lblTotalLiquido);

        totais.add(new JLabel("Pagamento:"));
        cbPagamento = new JComboBox<>(MetodoPagamento.values());
        totais.add(cbPagamento);

        add(totais, BorderLayout.SOUTH);

        // Botões
        JPanel botoes = new JPanel();

        JButton btnFinalizar = new JButton("Finalizar Venda");
        btnFinalizar.addActionListener(e -> finalizarVenda());
        botoes.add(btnFinalizar);

        JButton btnCancelar = new JButton("Cancelar");
        botoes.add(btnCancelar);

        add(botoes, BorderLayout.SOUTH);
        
        
    }

    private void adicionarProduto() {
        try {
            String codigo = txtCodigo.getText().trim();
            Produto p = produtoDAO.buscarPorCodigo(codigo);

            if (p == null) {
                JOptionPane.showMessageDialog(this, "Produto não encontrado!");
                return;
            }

            vendaAtual.addItemInteligente(p, BigDecimal.ONE);

            modelo.addRow(new Object[]{
                    p.getNome(),
                    1,
                    p.getPrecoBase(),
                    p.isTemIva() ? p.getTaxaIva() : "0",
                    vendaAtual.getItens().get(vendaAtual.getItens().size() - 1).getTotalLiquido()
            });

            atualizarTotaisUI();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void atualizarTotaisUI() {
        lblTotalBruto.setText(vendaAtual.getTotalBruto().toString());
        lblTotalIva.setText(vendaAtual.getTotalIva().toString());
        lblTotalLiquido.setText(vendaAtual.getTotalLiquido().toString());
    }

    private void finalizarVenda() {
        try {
            vendaAtual.setMetodoPagamento((MetodoPagamento) cbPagamento.getSelectedItem());
            vendaAtual.setDataHora(LocalDateTime.now());

            vendaDAO.salvar(vendaAtual);

            JOptionPane.showMessageDialog(this, "Venda gravada com sucesso!");

            vendaAtual = new Venda();
            modelo.setRowCount(0);
            atualizarTotaisUI();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao gravar venda!");
        }
    }

    public static void main(String[] args) {
        new CaixaPOS().setVisible(true);
    }
    
    
}
