package dao;

import model.Produto;
import model.StockMovimento;
import enums.TipoMovimentoStock;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * DAO para registos de movimentos de stock.
 * Método principal: registrarMovimento(...) usado pela UI de entrada de stock.
 */
public class StockMovimentoDAO {

    private Connection getConnection() throws SQLException {
        return ConnectionFactory.getConnection();
    }

    /**
     * Regista um movimento de stock na tabela stock_movimento.
     *
     * @param produto       o produto envolvendo o movimento (deve ter id)
     * @param quantidade    quantidade movimentada (positivo para entrada, negativo para saída)
     * @param stockAntes    stock antes da movimentação
     * @param stockDepois   stock depois da movimentação
     * @param tipo          tipo do movimento (ENTRADA, SAIDA_VENDA, AJUSTE, DEV_VENDA, DEV_COMPRA)
     * @param observacao    texto livre (ex: "Compra ao fornecedor")
     * @throws SQLException em caso de erro de BD
     */
    public void registrarMovimento(Produto produto,
                                  BigDecimal quantidade,
                                  BigDecimal stockAntes,
                                  BigDecimal stockDepois,
                                  TipoMovimentoStock tipo,
                                  String observacao) throws SQLException {

        String sql = "INSERT INTO stock_movimento " +
                "(id_produto, data_hora, tipo_movimento, quantidade, stock_antes, stock_depois, observacao) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, produto.getId());

            // data_hora: usar timestamp actual
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault())));

            ps.setString(3, tipo.name());
            ps.setBigDecimal(4, quantidade);
            ps.setBigDecimal(5, stockAntes);
            ps.setBigDecimal(6, stockDepois);
            ps.setString(7, observacao);

            ps.executeUpdate();

            // opcional: retornar id gerado e guardar no objecto StockMovimento se necessário
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    // se quiseres, podes criar e devolver o objecto StockMovimento aqui
                }
            }
        }
    }

    /**
     * Regista um movimento e, opcionalmente, actualiza o stock do produto (se necessário).
     * Não actualiza o produto por padrão — deixar isso ao ProdutoDAO/trigger para evitar duplicação.
     */
    public void registrarMovimentoSemAtualizarProduto(Produto produto,
                                                      BigDecimal quantidade,
                                                      BigDecimal stockAntes,
                                                      BigDecimal stockDepois,
                                                      TipoMovimentoStock tipo,
                                                      String observacao) throws SQLException {
        registrarMovimento(produto, quantidade, stockAntes, stockDepois, tipo, observacao);
    }

    /**
     * Consulta simples de movimentos por produto (útil para UI de histórico).
     */
    public ResultSet buscarMovimentosPorProduto(int idProduto) throws SQLException {
        String sql = "SELECT * FROM stock_movimento WHERE id_produto = ? ORDER BY data_hora DESC";
        Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idProduto);
        // devolvemos ResultSet direto; caller deve fechar rs e connection (ou podes adaptar para devolver List<StockMovimento>)
        return ps.executeQuery();
    }

    // Se preferires, posso escrever também um método que retorne List<StockMovimento>.
}
