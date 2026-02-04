package dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class RelatorioDAO {

    public Map<String, BigDecimal> totalVendasPorCaixa(LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        String sql = """
            SELECT c.nome AS caixa, SUM(v.total_liquido) AS total
            FROM venda v
            JOIN caixa c ON c.id_caixa = v.id_caixa
            WHERE v.estado = 'NORMAL'
              AND v.data_hora BETWEEN ? AND ?
            GROUP BY c.nome
        """;

        Map<String, BigDecimal> resultado = new LinkedHashMap<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fim));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString("caixa"), rs.getBigDecimal("total"));
                }
            }
        }

        return resultado;
    }

    public Map<String, BigDecimal> totalVendasPorMetodoPagamento(LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        String sql = """
            SELECT metodo_pagamento, SUM(total_liquido) AS total
            FROM venda
            WHERE estado = 'NORMAL'
              AND data_hora BETWEEN ? AND ?
            GROUP BY metodo_pagamento
        """;

        Map<String, BigDecimal> resultado = new LinkedHashMap<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fim));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString("metodo_pagamento"), rs.getBigDecimal("total"));
                }
            }
        }

        return resultado;
    }

    // Exemplo: top N produtos mais vendidos
    public List<ProdutoResumoVenda> produtosMaisVendidos(LocalDateTime inicio, LocalDateTime fim, int limite) throws SQLException {
        String sql = """
            SELECT p.id_produto, p.nome, SUM(vi.quantidade) AS quantidade_total,
                   SUM(vi.total_liquido) AS valor_total
            FROM venda_item vi
            JOIN venda v ON v.id_venda = vi.id_venda
            JOIN produto p ON p.id_produto = vi.id_produto
            WHERE v.estado = 'NORMAL'
              AND v.data_hora BETWEEN ? AND ?
            GROUP BY p.id_produto, p.nome
            ORDER BY quantidade_total DESC
            LIMIT ?
        """;

        List<ProdutoResumoVenda> lista = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fim));
            ps.setInt(3, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProdutoResumoVenda dto = new ProdutoResumoVenda();
                    dto.setIdProduto(rs.getInt("id_produto"));
                    dto.setNome(rs.getString("nome"));
                    dto.setQuantidadeTotal(rs.getBigDecimal("quantidade_total"));
                    dto.setValorTotal(rs.getBigDecimal("valor_total"));
                    lista.add(dto);
                }
            }
        }

        return lista;
    }
}