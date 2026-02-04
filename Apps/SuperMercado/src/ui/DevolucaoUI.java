package ui;

import dao.DevolucaoDAO;
import dao.VendaDAO;
import model.*;
import enums.TipoDevolucao;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DevolucaoUI extends JFrame {

    private JTextField txtIdVenda, txtQuantidade;
    private JTable tabela;
    private DefaultTableModel modelo;

    private Venda venda;
    private VendaDAO vendaDAO = new VendaDAO();
    private DevolucaoDAO devDAO = new DevolucaoDAO();

    public DevolucaoUI() {
        setTitle("Devolução/Troca");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
    }

    private void initUI() {
        JPanel top = new JPanel(new FlowLayout());
        top.add(new JLabel("ID Venda:"));
        txtIdVenda = new JTextField(6);
        top.add(txtIdVenda);

        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscarVenda());
        top.add(btnBuscar);

        add(top, BorderLayout.NORTH);

        modelo = new DefaultTableModel(
                new String[]{"ID Item", "Produto", "Qtd Vendida", "Total"}, 0);

        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout());

        bottom.add(new JLabel("Quantidade a devolver:"));
        txtQuantidade = new JTextField(6);
        bottom.add(txtQuantidade);

        JButton btnDevolver = new JButton("Devolver");
        btnDevolver.addActionListener(e -> registrarDevolucao(TipoDevolucao.DEVOLUCAO));
        bottom.add(btnDevolver);

        JButton btnTrocar = new JButton("Troca");
        btnTrocar.addActionListener(e -> registrarDevolucao(TipoDevolucao.TROCA));
        bottom.add(btnTrocar);

        add(bottom, BorderLayout.SOUTH);
    }

    private void buscarVenda() {
        try {
            int id = Integer.parseInt(txtIdVenda.getText());
            venda = vendaDAO.buscarPorId(id);  // PRECISAS implementar buscarPorId

            modelo.setRowCount(0);
            for (VendaItem item : venda.getItens()) {
                modelo.addRow(new Object[]{
                        item.getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getTotalLiquido()
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar venda");
        }
    }

    private void registrarDevolucao(TipoDevolucao tipo) {
        try {
            int linha = tabela.getSelectedRow();
            if (linha == -1) {
                JOptionPane.showMessageDialog(this, "Selecione um item da venda.");
                return;
            }

            int idVendaItem = (int) modelo.getValueAt(linha, 0);

            VendaItem itemOriginal = venda.getItens()
                    .stream()
                    .filter(i -> i.getId().equals(idVendaItem))
                    .findFirst()
                    .get();

            BigDecimal qtd = new BigDecimal(txtQuantidade.getText());

            Devolucao dev = new Devolucao();
            dev.setVendaOriginal(venda);
            dev.setCaixa(venda.getCaixa());
            dev.setTipo(tipo);
            dev.setDataHora(LocalDateTime.now());
            dev.setMotivo("Cliente devolveu");

            DevolucaoItem di = new DevolucaoItem();
            di.setProduto(itemOriginal.getProduto());
            di.setQuantidade(qtd);
            di.setValorLiquido(itemOriginal.getPrecoUnitario().multiply(qtd));
            di.setVendaItem(itemOriginal);

            dev.addItem(di);

            devDAO.salvar(dev);

            JOptionPane.showMessageDialog(this, "Operação registada!");

            dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao registrar devolução.");
        }
    }
}
