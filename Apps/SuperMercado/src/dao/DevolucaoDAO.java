package dao;

import model.Devolucao;
import model.DevolucaoItem;

import java.sql.*;

public class DevolucaoDAO {

    public void salvar(Devolucao dev) throws SQLException {
        String sqlDev = "INSERT INTO devolucao (id_venda, id_caixa, data_hora, tipo, motivo) " +
                        "VALUES (?, ?, ?, ?, ?)";

        String sqlItem = "INSERT INTO devolucao_item " +
                "(id_devolucao, id_venda_item, id_produto, quantidade, valor_liquido) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psDev = null;
        PreparedStatement psItem = null;

        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            // 1) Inserir devolução
            psDev = conn.prepareStatement(sqlDev, Statement.RETURN_GENERATED_KEYS);
            psDev.setInt(1, dev.getVendaOriginal().getId());
            psDev.setInt(2, dev.getCaixa().getId());
            psDev.setTimestamp(3, Timestamp.valueOf(dev.getDataHora()));
            psDev.setString(4, dev.getTipo().name());
            psDev.setString(5, dev.getMotivo());

            psDev.executeUpdate();

            ResultSet rsDev = psDev.getGeneratedKeys();
            if (rsDev.next()) dev.setId(rsDev.getInt(1));

            // 2) Inserir itens de devolução
            psItem = conn.prepareStatement(sqlItem, Statement.RETURN_GENERATED_KEYS);

            for (DevolucaoItem item : dev.getItens()) {
                psItem.setInt(1, dev.getId());
                psItem.setInt(2, item.getVendaItem().getId());
                psItem.setInt(3, item.getProduto().getId());
                psItem.setBigDecimal(4, item.getQuantidade());
                psItem.setBigDecimal(5, item.getValorLiquido());

                psItem.executeUpdate();

                ResultSet rsItem = psItem.getGeneratedKeys();
                if (rsItem.next()) {
                    item.setId(rsItem.getInt(1));
                }
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (psItem != null) psItem.close();
            if (psDev != null) psDev.close();
            if (conn != null) conn.close();
        }
    }
}
