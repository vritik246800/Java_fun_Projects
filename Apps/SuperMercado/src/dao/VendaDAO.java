package dao;

import model.Caixa;
import model.Cliente;
import model.Produto;
import model.Venda;
import model.VendaItem;

import java.sql.*;

import enums.CategoriaProduto;
import enums.EstadoVenda;
import enums.MetodoPagamento;
import enums.TipoVenda;

public class VendaDAO {

	public void salvar(Venda venda) throws SQLException {

	    // 🔥 1) Recalcular todos os itens da venda
	    for (VendaItem item : venda.getItens()) {
	        item.calcularTotais();   // calcula total_bruto, total_iva, total_liquido
	    }

	    // 🔥 2) Recalcular totais da venda
	    venda.calcularTotais();

	    String sqlVenda = "INSERT INTO venda (id_caixa, id_cliente, data_hora, metodo_pagamento, " +
	            "total_bruto, total_iva, total_liquido, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	    String sqlItem = "INSERT INTO venda_item (id_venda, id_produto, quantidade, preco_unitario, " +
	            "taxa_iva, total_bruto, total_iva, total_liquido) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	    Connection conn = null;
	    PreparedStatement psVenda = null;
	    PreparedStatement psItem = null;

	    try {
	        conn = ConnectionFactory.getConnection();
	        conn.setAutoCommit(false);

	        // 🔵 3) Inserir venda
	        psVenda = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS);

	        psVenda.setInt(1, venda.getCaixa().getId());
	        if (venda.getCliente() != null) {
	            psVenda.setInt(2, venda.getCliente().getId());
	        } else {
	            psVenda.setNull(2, Types.INTEGER);
	        }

	        psVenda.setTimestamp(3, Timestamp.valueOf(venda.getDataHora()));
	        psVenda.setString(4, venda.getMetodoPagamento().name());
	        psVenda.setBigDecimal(5, venda.getTotalBruto());
	        psVenda.setBigDecimal(6, venda.getTotalIva());
	        psVenda.setBigDecimal(7, venda.getTotalLiquido());
	        psVenda.setString(8, venda.getEstado().name());

	        psVenda.executeUpdate();

	        ResultSet rsKeys = psVenda.getGeneratedKeys();
	        if (rsKeys.next()) {
	            venda.setId(rsKeys.getInt(1));
	        }

	        // 🔵 4) Inserir itens
	        psItem = conn.prepareStatement(sqlItem, Statement.RETURN_GENERATED_KEYS);

	        for (VendaItem item : venda.getItens()) {

	            psItem.setInt(1, venda.getId());
	            psItem.setInt(2, item.getProduto().getId());
	            psItem.setBigDecimal(3, item.getQuantidade());
	            psItem.setBigDecimal(4, item.getPrecoUnitario());
	            psItem.setBigDecimal(5, item.getTaxaIva());
	            psItem.setBigDecimal(6, item.getTotalBruto());
	            psItem.setBigDecimal(7, item.getTotalIva());
	            psItem.setBigDecimal(8, item.getTotalLiquido());

	            psItem.executeUpdate();

	            ResultSet rsItemKeys = psItem.getGeneratedKeys();
	            if (rsItemKeys.next()) {
	                item.setId(rsItemKeys.getInt(1));
	            }
	        }

	        conn.commit();

	    } catch (SQLException e) {
	        if (conn != null) conn.rollback();
	        throw e;

	    } finally {
	        if (psItem != null) psItem.close();
	        if (psVenda != null) psVenda.close();
	        if (conn != null) conn.close();
	    }
	}
	
	public Venda buscarPorId(int idVenda) throws SQLException {

	    String sqlVenda = "SELECT * FROM venda WHERE id_venda = ?";
	    String sqlItens = """
	        SELECT vi.*, p.nome, p.codigo, p.categoria, p.tipo_venda, 
	               p.preco_base, p.tem_iva, p.taxa_iva, p.stock_atual 
	        FROM venda_item vi
	        JOIN produto p ON p.id_produto = vi.id_produto
	        WHERE vi.id_venda = ?
	    """;

	    Connection conn = null;
	    PreparedStatement psVenda = null;
	    PreparedStatement psItens = null;
	    ResultSet rsVenda = null;
	    ResultSet rsItens = null;

	    try {
	        conn = ConnectionFactory.getConnection();

	        // 1️⃣ Buscar os dados da venda
	        psVenda = conn.prepareStatement(sqlVenda);
	        psVenda.setInt(1, idVenda);
	        rsVenda = psVenda.executeQuery();

	        if (!rsVenda.next()) {
	            return null; // venda não existe
	        }

	        Venda venda = new Venda();
	        venda.setId(idVenda);

	        // carregar caixa (apenas ID, depois podes buscar completo se quiseres)
	        Caixa cx = new Caixa();
	        cx.setId(rsVenda.getInt("id_caixa"));
	        venda.setCaixa(cx);

	        // cliente pode ser null
	        int idCliente = rsVenda.getInt("id_cliente");
	        if (!rsVenda.wasNull()) {
	            Cliente c = new Cliente();
	            c.setId(idCliente);
	            venda.setCliente(c);
	        }

	        venda.setDataHora(rsVenda.getTimestamp("data_hora").toLocalDateTime());
	        venda.setMetodoPagamento(MetodoPagamento.valueOf(rsVenda.getString("metodo_pagamento")));
	        venda.setTotalBruto(rsVenda.getBigDecimal("total_bruto"));
	        venda.setTotalIva(rsVenda.getBigDecimal("total_iva"));
	        venda.setTotalLiquido(rsVenda.getBigDecimal("total_liquido"));
	        venda.setEstado(EstadoVenda.valueOf(rsVenda.getString("estado")));

	        // 2️⃣ Buscar itens da venda
	        psItens = conn.prepareStatement(sqlItens);
	        psItens.setInt(1, idVenda);
	        rsItens = psItens.executeQuery();

	        while (rsItens.next()) {

	            VendaItem item = new VendaItem();
	            item.setId(rsItens.getInt("id_venda_item"));
	            item.setVenda(venda);

	            // produto
	            Produto p = new Produto();
	            p.setId(rsItens.getInt("id_produto"));
	            p.setNome(rsItens.getString("nome"));
	            p.setCodigo(rsItens.getString("codigo"));
	            p.setCategoria(CategoriaProduto.valueOf(rsItens.getString("categoria")));
	            p.setTipoVenda(TipoVenda.valueOf(rsItens.getString("tipo_venda")));
	            p.setPrecoBase(rsItens.getBigDecimal("preco_base"));
	            p.setTemIva(rsItens.getBoolean("tem_iva"));
	            p.setTaxaIva(rsItens.getBigDecimal("taxa_iva"));
	            p.setStockAtual(rsItens.getBigDecimal("stock_atual"));

	            item.setProduto(p);

	            // valores da venda_item
	            item.setQuantidade(rsItens.getBigDecimal("quantidade"));
	            item.setPrecoUnitario(rsItens.getBigDecimal("preco_unitario"));
	            item.setTaxaIva(rsItens.getBigDecimal("taxa_iva"));
	            item.setTotalBruto(rsItens.getBigDecimal("total_bruto"));
	            item.setTotalIva(rsItens.getBigDecimal("total_iva"));
	            item.setTotalLiquido(rsItens.getBigDecimal("total_liquido"));

	            venda.getItens().add(item);
	        }

	        return venda;

	    } finally {

	        if (rsItens != null) rsItens.close();
	        if (psItens != null) psItens.close();

	        if (rsVenda != null) rsVenda.close();
	        if (psVenda != null) psVenda.close();

	        if (conn != null) conn.close();
	    }
	}
	

}