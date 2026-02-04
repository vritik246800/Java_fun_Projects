package dao;

import java.math.BigDecimal;

public class ProdutoResumoVenda {

    private int idProduto;
    private String nome;
    private BigDecimal quantidadeTotal;
    private BigDecimal valorTotal;

    public int getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(int idProduto) {
        this.idProduto = idProduto;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public void setQuantidadeTotal(BigDecimal quantidadeTotal) {
        this.quantidadeTotal = quantidadeTotal;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    @Override
    public String toString() {
        return "ProdutoResumoVenda {" +
                "idProduto=" + idProduto +
                ", nome='" + nome + '\'' +
                ", quantidadeTotal=" + quantidadeTotal +
                ", valorTotal=" + valorTotal +
                '}';
    }
}
