package model;

import java.math.BigDecimal;

public class VendaItem {

    private Integer id;
    private Venda venda;
    private Produto produto;
    private BigDecimal quantidade;     // unidade ou kg
    private BigDecimal precoUnitario;  // sem IVA
    private BigDecimal taxaIva;
    private BigDecimal totalBruto;
    private BigDecimal totalIva;
    private BigDecimal totalLiquido;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Venda getVenda() { return venda; }
    public void setVenda(Venda venda) { this.venda = venda; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public BigDecimal getTaxaIva() { return taxaIva; }
    public void setTaxaIva(BigDecimal taxaIva) { this.taxaIva = taxaIva; }

    public BigDecimal getTotalBruto() { return totalBruto; }
    public void setTotalBruto(BigDecimal totalBruto) { this.totalBruto = totalBruto; }

    public BigDecimal getTotalIva() { return totalIva; }
    public void setTotalIva(BigDecimal totalIva) { this.totalIva = totalIva; }

    public BigDecimal getTotalLiquido() { return totalLiquido; }
    public void setTotalLiquido(BigDecimal totalLiquido) { this.totalLiquido = totalLiquido; }

    public void calcularTotais() {
        this.totalBruto = precoUnitario.multiply(quantidade);

        this.totalIva = totalBruto
                .multiply(taxaIva)
                .divide(new BigDecimal("100"));

        this.totalLiquido = totalBruto.add(totalIva);
    }
    

}