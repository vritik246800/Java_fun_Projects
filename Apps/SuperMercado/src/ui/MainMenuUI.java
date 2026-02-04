package ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class MainMenuUI extends JFrame {

    private JTree menuTree;

    public MainMenuUI() {
        setTitle("Sistema POS - Menu Principal");
        setSize(750, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initUI();
    }

    private void initUI() {

        // ---------------- CRIAR TREE ROOT ----------------
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Menu Principal");

        // Produtos
        DefaultMutableTreeNode produtos = new DefaultMutableTreeNode("Produtos");
        produtos.add(new DefaultMutableTreeNode("Cadastrar Produto"));
        produtos.add(new DefaultMutableTreeNode("Listar / Editar Produtos"));
        produtos.add(new DefaultMutableTreeNode("Consulta Stock"));
        produtos.add(new DefaultMutableTreeNode("Entrada de Stock"));
        root.add(produtos);

        // Vendas
        DefaultMutableTreeNode vendas = new DefaultMutableTreeNode("Vendas");
        vendas.add(new DefaultMutableTreeNode("Abrir Caixa / POS"));
        vendas.add(new DefaultMutableTreeNode("Devolução / Troca"));
        root.add(vendas);

        // Sistema
        DefaultMutableTreeNode sistema = new DefaultMutableTreeNode("Sistema");
        sistema.add(new DefaultMutableTreeNode("Sair"));
        root.add(sistema);


        menuTree = new JTree(root);
        menuTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Ação ao selecionar
        menuTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) menuTree.getLastSelectedPathComponent();
            if (node == null || node.isRoot()) return;

            String opcao = node.getUserObject().toString();

            switch (opcao) {
                case "Cadastrar Produto":
                    new ProdutoCadastroUI().setVisible(true);
                    break;

                case "Listar / Editar Produtos":
                    new ProdutoListaUI().setVisible(true);
                    break;

                case "Consulta Stock":
                    new StockConsultaUI().setVisible(true);
                    break;

                case "Entrada de Stock":
                    new EntradaStockUI().setVisible(true);
                    break;

                case "Abrir Caixa / POS":
                    new CaixaPOS().setVisible(true);
                    break;

                case "Devolução / Troca":
                    new DevolucaoUI().setVisible(true);
                    break;

                case "Sair":
                    System.exit(0);
                    break;
            }
        });


        // Layout: árvore à esquerda + painel a direita
        setLayout(new BorderLayout());

        JScrollPane scrollTree = new JScrollPane(menuTree);
        scrollTree.setPreferredSize(new Dimension(220, 0));

        add(scrollTree, BorderLayout.WEST);

        JLabel label = new JLabel("Bem-vindo ao Sistema POS", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(22f));

        add(label, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        new MainMenuUI().setVisible(true);
    }
}
