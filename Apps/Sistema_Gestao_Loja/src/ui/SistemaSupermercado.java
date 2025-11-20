package ui;

import modelo.Cliente;
import modelo.Produto;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SistemaSupermercado extends JFrame {

    private List<Produto> produtos = new ArrayList<>();
    private List<Cliente> clientes = new ArrayList<>();

    public SistemaSupermercado() {
        setTitle("Sistema Supermercado");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        inicializarDadosTeste();

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Vendas", new PainelVendas(produtos, clientes));
        tabs.addTab("Produtos", new PainelProdutos(produtos));
        tabs.addTab("Clientes", new PainelClientes(clientes));
        tabs.addTab("Admin", new PainelAdmin(produtos, clientes));
        tabs.addTab("📊 Estatísticas", new PainelEstatisticas(produtos, clientes));

        add(tabs);
    }

    private void inicializarDadosTeste() {
        Cliente c1 = new Cliente("Consumidor Final", "999999999", "Individual");
        clientes.add(c1);

        produtos.add(new Produto("Arroz 1kg", 50, 20, false, 100));
        produtos.add(new Produto("Teclado USB", 500, 30, true, 10));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new SistemaSupermercado().setVisible(true));
    }
}
