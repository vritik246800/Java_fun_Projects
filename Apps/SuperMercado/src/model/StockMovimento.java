package model;

import enums.TipoMovimentoStock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockMovimento {

    private Integer id;
    private Produto produto;
    private LocalDateTime dataHora;
    private TipoMovimentoStock tipo;
    private BigDecimal quantidade;      // + entrada, - saída
    private BigDecimal stockAntes;
    private BigDecimal stockDepois;
    private VendaItem vendaItem;        // opcional
    private DevolucaoItem devolucaoItem;// opcional
    private String observacao;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public TipoMovimentoStock getTipo() { return tipo; }
    public void setTipo(TipoMovimentoStock tipo) { this.tipo = tipo; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getStockAntes() { return stockAntes; }
    public void setStockAntes(BigDecimal stockAntes) { this.stockAntes = stockAntes; }

    public BigDecimal getStockDepois() { return stockDepois; }
    public void setStockDepois(BigDecimal stockDepois) { this.stockDepois = stockDepois; }

    public VendaItem getVendaItem() { return vendaItem; }
    public void setVendaItem(VendaItem vendaItem) { this.vendaItem = vendaItem; }

    public DevolucaoItem getDevolucaoItem() { return devolucaoItem; }
    public void setDevolucaoItem(DevolucaoItem devolucaoItem) { this.devolucaoItem = devolucaoItem; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}