package dao;

import model.Produto;
import enums.CategoriaProduto;
import enums.TipoVenda;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    private Connection getConnection() throws SQLException {
        return ConnectionFactory.getConnection();
    }

    // 🔎 Buscar produto pelo código (USADO NO CaixaPOS)
    public Produto buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT * FROM produto WHERE codigo = ? AND ativo = TRUE";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearProduto(rs);
            }
        }

        return null;
    }

    // 🔎 Buscar produto pelo ID
    public Produto buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM produto WHERE id_produto = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearProduto(rs);
            }
        }

        return null;
    }

    // 📌 Listar todos produtos ativos
    public List<Produto> listarTodos() throws SQLException {
        String sql = "SELECT * FROM produto WHERE ativo = TRUE";

        List<Produto> lista = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProduto(rs));
            }
        }

        return lista;
    }

    // 🔄 Atualizar stock manualmente (se não usares triggers)
    public void atualizarStock(int idProduto, BigDecimal novoStock) throws SQLException {

        String sql = "UPDATE produto SET stock_atual = ? WHERE id_produto = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, novoStock);
            ps.setInt(2, idProduto);

            ps.executeUpdate();
        }
    }

    // 🧩 Função privada para mapear o produto
    private Produto mapearProduto(ResultSet rs) throws SQLException {

        Produto p = new Produto();

        p.setId(rs.getInt("id_produto"));
        p.setCodigo(rs.getString("codigo"));
        p.setNome(rs.getString("nome"));

        p.setCategoria(CategoriaProduto.valueOf(rs.getString("categoria")));
        p.setTipoVenda(TipoVenda.valueOf(rs.getString("tipo_venda")));

        p.setPrecoBase(rs.getBigDecimal("preco_base"));
        p.setTemIva(rs.getBoolean("tem_iva"));
        p.setTaxaIva(rs.getBigDecimal("taxa_iva"));
        p.setStockAtual(rs.getBigDecimal("stock_atual"));
        p.setAtivo(rs.getBoolean("ativo"));

        return p;
    }
    
    public void inserirProduto(Produto p) throws SQLException {
        String sql = """
            INSERT INTO produto
            (codigo, nome, categoria, tipo_venda, preco_base, tem_iva, taxa_iva, stock_atual, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNome());
            ps.setString(3, p.getCategoria().name());
            ps.setString(4, p.getTipoVenda().name());
            ps.setBigDecimal(5, p.getPrecoBase());
            ps.setBoolean(6, p.isTemIva());
            ps.setBigDecimal(7, p.getTaxaIva());
            ps.setBigDecimal(8, p.getStockAtual());
            ps.setBoolean(9, p.isAtivo());

            ps.executeUpdate();
        }
    }
    public void atualizarProduto(Produto p) throws SQLException {

        String sql = """
            UPDATE produto
            SET nome = ?,
                categoria = ?,
                tipo_venda = ?,
                preco_base = ?,
                tem_iva = ?,
                taxa_iva = ?,
                stock_atual = ?,
                ativo = ?
            WHERE id_produto = ?
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNome());
            ps.setString(2, p.getCategoria().name());
            ps.setString(3, p.getTipoVenda().name());
            ps.setBigDecimal(4, p.getPrecoBase());
            ps.setBoolean(5, p.isTemIva());
            ps.setBigDecimal(6, p.getTaxaIva());
            ps.setBigDecimal(7, p.getStockAtual());
            ps.setBoolean(8, p.isAtivo());
            ps.setInt(9, p.getId());

            ps.executeUpdate();
        }
    }

    
}
