
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class MainApp extends JFrame {

    private CardLayout card;
    private JPanel painelCentral;

    public MainApp() {
        setTitle("Sistema de Cálculo de Preços");
        
        setIconImage(
        	    new ImageIcon("Image/price.png").getImage()
        	);
        
        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(criarMenuTree(), BorderLayout.WEST);
        add(criarPainelCentral(), BorderLayout.CENTER);
    }

    private JScrollPane criarMenuTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Calculadoras");
        root.add(new DefaultMutableTreeNode("Preço Base"));
        root.add(new DefaultMutableTreeNode("Preço Final"));

        JTree tree = new JTree(root);
        tree.setRootVisible(true);

        tree.addTreeSelectionListener(e -> {
            var node = tree.getLastSelectedPathComponent();
            if (node == null) return;

            switch (node.toString()) {
                case "Preço Base" -> card.show(painelCentral, "BASE");
                case "Preço Final" -> card.show(painelCentral, "FINAL");
            }
        });

        JScrollPane sp = new JScrollPane(tree);
        sp.setPreferredSize(new Dimension(220, 0));
        return sp;
    }

    private JPanel criarPainelCentral() {
        card = new CardLayout();
        painelCentral = new JPanel(card);

        painelCentral.add(new PainelPrecoBase(), "BASE");
        painelCentral.add(new PainelPrecoFinal(), "FINAL");

        return painelCentral;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new MainApp().setVisible(true));
    }
}
