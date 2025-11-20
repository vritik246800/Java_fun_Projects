package database;

import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;

public class VendaDAO {

    public static void registrarVenda(double total, DefaultTableModel tabela) throws Exception {

        // Inserir venda
        PreparedStatement ps = Conexao.get().prepareStatement(
                "INSERT INTO vendas(data, total) VALUES(datetime('now'), ?)");
        ps.setDouble(1, total);
        ps.execute();

        // Buscar ID da venda
        var rs = Conexao.get().createStatement()
                .executeQuery("SELECT last_insert_rowid() AS id");
        int idVenda = rs.getInt("id");

        // Inserir itens
        for (int i = 0; i < tabela.getRowCount(); i++) {

            PreparedStatement ps2 = Conexao.get().prepareStatement(
                "INSERT INTO venda_itens(idVenda, nomeProduto, preco, quantidade, subtotal) " +
                "VALUES(?,?,?,?,?)"
            );

            ps2.setInt(1, idVenda);
            ps2.setString(2, tabela.getValueAt(i, 0).toString());
            ps2.setDouble(3, (double) tabela.getValueAt(i, 2));
            ps2.setInt(4, (int) tabela.getValueAt(i, 1));
            ps2.setDouble(5, (double) tabela.getValueAt(i, 3));
            ps2.execute();
        }
    }
}
