package model;

import enums.CategoriaProduto;
import enums.TipoVenda;

import java.math.BigDecimal;

public class Produto {

    private Integer id;
    private String codigo;
    private String nome;
    private CategoriaProduto categoria;
    private TipoVenda tipoVenda;
    private BigDecimal precoBase;    // preço sem IVA
    private boolean temIva;
    private BigDecimal taxaIva;      // % quando aplicável
    private BigDecimal stockAtual;   // unidades ou kg
    private boolean ativo = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public CategoriaProduto getCategoria() { return categoria; }
    public void setCategoria(CategoriaProduto categoria) { this.categoria = categoria; }

    public TipoVenda getTipoVenda() { return tipoVenda; }
    public void setTipoVenda(TipoVenda tipoVenda) { this.tipoVenda = tipoVenda; }

    public BigDecimal getPrecoBase() { return precoBase; }
    public void setPrecoBase(BigDecimal precoBase) { this.precoBase = precoBase; }

    public boolean isTemIva() { return temIva; }
    public void setTemIva(boolean temIva) { this.temIva = temIva; }

    public BigDecimal getTaxaIva() { return taxaIva; }
    public void setTaxaIva(BigDecimal taxaIva) { this.taxaIva = taxaIva; }

    public BigDecimal getStockAtual() { return stockAtual; }
    public void setStockAtual(BigDecimal stockAtual) { this.stockAtual = stockAtual; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}