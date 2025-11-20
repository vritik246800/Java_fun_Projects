package ui;

import modelo.Cliente;
import modelo.Produto;
import utils.PDFUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import database.VendaDAO;

import java.awt.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PainelVendas extends JPanel {

    private List<Produto> produtos;
    private List<Cliente> clientes;

    private DefaultTableModel modeloTabelaVenda;
    private JComboBox<Produto> comboProdutos;
    private JComboBox<Cliente> comboClientes;
    private JLabel lblTotal;
    private double total = 0;

    public PainelVendas(List<Produto> produtos, List<Cliente> clientes) {
        this.produtos = produtos;
        this.clientes = clientes;

        setLayout(new BorderLayout());

        add(criarTopo(), BorderLayout.NORTH);
        add(criarTabela(), BorderLayout.CENTER);
        add(criarRodape(), BorderLayout.SOUTH);
    }

    private JPanel criarTopo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        comboClientes = new JComboBox<>(clientes.toArray(new Cliente[0]));
        comboProdutos = new JComboBox<>(produtos.toArray(new Produto[0]));
        JTextField txtQtd = new JTextField("1", 5);

        JButton btnAdd = new JButton("Adicionar");

        btnAdd.addActionListener(e -> {
            Produto prod = (Produto) comboProdutos.getSelectedItem();
            int qtd = Integer.parseInt(txtQtd.getText());

            double subtotal = prod.getPrecoFinal() * qtd;
            modeloTabelaVenda.addRow(new Object[]{
                    prod.getNome(),
                    qtd,
                    prod.getPrecoFinal(),
                    subtotal
            });

            total += subtotal;
            lblTotal.setText(String.format("TOTAL: %.2f MT", total));
        });

        p.add(new JLabel("Cliente:"));
        p.add(comboClientes);
        p.add(new JLabel("Produto:"));
        p.add(comboProdutos);
        p.add(new JLabel("Qtd:"));
        p.add(txtQtd);
        p.add(btnAdd);

        return p;
    }

    private JScrollPane criarTabela() {
        String[] colunas = {"Produto", "Qtd", "Preço", "Subtotal"};
        modeloTabelaVenda = new DefaultTableModel(colunas, 0);
        JTable tabela = new JTable(modeloTabelaVenda);
        return new JScrollPane(tabela);
    }

    private JPanel criarRodape() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        lblTotal = new JLabel("TOTAL: 0.00 MT");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));

        JButton btnFinalizar = new JButton("Finalizar Venda");

        btnFinalizar.addActionListener(e -> gerarRecibo());

        p.add(lblTotal);
        p.add(btnFinalizar);

        return p;
    }

    private void gerarRecibo() {
        if (modeloTabelaVenda.getRowCount() == 0) return;

        StringBuilder recibo = new StringBuilder();
        recibo.append("=== SUPERMERCADO JAVA ===\n");
        recibo.append("Data: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) + "\n");
        recibo.append("Cliente: " + comboClientes.getSelectedItem() + "\n");
        recibo.append("--------------------------------\n");

        for (int i = 0; i < modeloTabelaVenda.getRowCount(); i++) {
            String prod = modeloTabelaVenda.getValueAt(i, 0).toString();
            int qtd = (int) modeloTabelaVenda.getValueAt(i, 1);
            double sub = ((Number) modeloTabelaVenda.getValueAt(i, 3)).doubleValue();
            recibo.append(String.format("%-15s x%d   %.2f\n", prod, qtd, sub));
        }

        recibo.append("--------------------------------\n");
        recibo.append(String.format("TOTAL A PAGAR: %.2f MT\n", total));
        recibo.append("IVA incluído à taxa legal.\n");
        recibo.append("================================");

        // 1) Registar venda na BD
        try {
            int id = VendaDAO.registrarVenda(total, modeloTabelaVenda);
            System.out.println("Venda registada com id: " + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao registar venda: " + ex.getMessage());
        }

        // 2) Gerar PDF do recibo
        try {
            String nomeArquivo = "recibo_" + System.currentTimeMillis() + ".pdf";
            utils.PDFUtils.gerarPDFRecibo(recibo.toString(), nomeArquivo);
            Desktop.getDesktop().open(new File(nomeArquivo));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage());
        }

        // 3) Imprimir (dialog opcional)
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(new Printable() {
                @Override
                public int print(java.awt.Graphics graphics, PageFormat pageFormat, int pageIndex) {
                    if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                    int y = 100;
                    for (String linha : recibo.toString().split("\n")) {
                        graphics.drawString(linha, 50, y);
                        y += 15;
                    }
                    return Printable.PAGE_EXISTS;
                }
            });

            if (job.printDialog()) {
                job.print();
            }
        } catch (PrinterException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro na impressão: " + ex.getMessage());
        }

        // Limpar venda após processar
        modeloTabelaVenda.setRowCount(0);
        total = 0;
        lblTotal.setText("TOTAL: 0.00 MT");
    }


}
