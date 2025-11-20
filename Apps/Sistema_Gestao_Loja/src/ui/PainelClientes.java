package ui;

import modelo.Cliente;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PainelClientes extends JPanel {

    private List<Cliente> clientes;

    public PainelClientes(List<Cliente> clientes) {
        this.clientes = clientes;

        setLayout(new GridLayout(5, 2));
        setBorder(BorderFactory.createTitledBorder("Registo de Clientes"));

        JTextField txtNome = new JTextField();
        JTextField txtNif = new JTextField();
        JComboBox<String> comboTipo = new JComboBox<>(new String[]{"Individual", "Empresa"});
        JButton btnSalvar = new JButton("Salvar Cliente");

        btnSalvar.addActionListener(e -> {
            clientes.add(new Cliente(
                    txtNome.getText(),
                    txtNif.getText(),
                    comboTipo.getSelectedItem().toString()
            ));
            JOptionPane.showMessageDialog(this, "Cliente registado!");
            txtNome.setText("");
            txtNif.setText("");
        });

        add(new JLabel("Nome:")); add(txtNome);
        add(new JLabel("NIF:")); add(txtNif);
        add(new JLabel("Tipo:")); add(comboTipo);
        add(new JLabel("")); add(btnSalvar);
    }
}
