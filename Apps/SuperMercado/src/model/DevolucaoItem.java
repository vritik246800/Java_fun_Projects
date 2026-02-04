package model;

import java.math.BigDecimal;

public class DevolucaoItem {

    private Integer id;
    private Devolucao devolucao;
    private VendaItem vendaItem;
    private Produto produto;
    private BigDecimal quantidade;
    private BigDecimal valorLiquido;  // valor devolvido/creditado ao cliente

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }

    public VendaItem getVendaItem() { return vendaItem; }
    public void setVendaItem(VendaItem vendaItem) { this.vendaItem = vendaItem; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(BigDecimal valorLiquido) { this.valorLiquido = valorLiquido; }
}