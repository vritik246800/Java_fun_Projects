package ui;

import modelo.Cliente;
import modelo.Produto;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;

public class PainelAdmin extends JPanel {

    private List<Produto> produtos;
    private List<Cliente> clientes;

    public PainelAdmin(List<Produto> produtos, List<Cliente> clientes) {
        this.produtos = produtos;
        this.clientes = clientes;

        JButton btnBackup = new JButton("Fazer Backup");

        btnBackup.addActionListener(e -> fazerBackup());

        add(btnBackup);
    }

    private void fazerBackup() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("backup_" + System.currentTimeMillis() + ".txt"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                FileWriter fw = new FileWriter(fc.getSelectedFile());

                fw.write("BACKUP - " + new Date() + "\n");
                fw.write("Produtos: " + produtos.size() + "\n");
                fw.write("Clientes: " + clientes.size() + "\n");

                fw.close();
                JOptionPane.showMessageDialog(this, "Backup criado!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }
}
