package database;

import modelo.Produto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    public static void criarTabela() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS produtos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT,
                precoCusto REAL,
                margem REAL,
                incluiIVA INTEGER,
                stock INTEGER
            );
        """;

        Conexao.get().createStatement().execute(sql);
    }

    public static void inserir(Produto p) throws SQLException {
        String sql = "INSERT INTO produtos(nome, precoCusto, margem, incluiIVA, stock) VALUES (?,?,?,?,?)";
        PreparedStatement ps = Conexao.get().prepareStatement(sql);
        ps.setString(1, p.getNome());
        ps.setDouble(2, p.getPrecoCusto());
        ps.setDouble(3, p.getMargemLucro());
        ps.setBoolean(4, p.isIncluiIVA());
        ps.setInt(5, p.getStock());
        ps.execute();
    }

    public static List<Produto> listar() throws SQLException {
        List<Produto> lista = new ArrayList<>();

        ResultSet rs = Conexao.get().createStatement().executeQuery("SELECT * FROM produtos");

        while (rs.next()) {
            lista.add(new Produto(
                rs.getString("nome"),
                rs.getDouble("precoCusto"),
                rs.getDouble("margem"),
                rs.getBoolean("incluiIVA"),
                rs.getInt("stock")
            ));
        }

        return lista;
    }
}
