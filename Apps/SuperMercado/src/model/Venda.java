package model;

import enums.MetodoPagamento;
import enums.EstadoVenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venda {

    private Integer id;
    private Caixa caixa;
    private Cliente cliente;
    private LocalDateTime dataHora;
    private MetodoPagamento metodoPagamento;
    private BigDecimal totalBruto;
    private BigDecimal totalIva;
    private BigDecimal totalLiquido;
    private EstadoVenda estado = EstadoVenda.NORMAL;

    private List<VendaItem> itens = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Caixa getCaixa() { return caixa; }
    public void setCaixa(Caixa caixa) { this.caixa = caixa; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public MetodoPagamento getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }

    public BigDecimal getTotalBruto() { return totalBruto; }
    public void setTotalBruto(BigDecimal totalBruto) { this.totalBruto = totalBruto; }

    public BigDecimal getTotalIva() { return totalIva; }
    public void setTotalIva(BigDecimal totalIva) { this.totalIva = totalIva; }

    public BigDecimal getTotalLiquido() { return totalLiquido; }
    public void setTotalLiquido(BigDecimal totalLiquido) { this.totalLiquido = totalLiquido; }

    public EstadoVenda getEstado() { return estado; }
    public void setEstado(EstadoVenda estado) { this.estado = estado; }

    public List<VendaItem> getItens() { return itens; }
    public void addItem(VendaItem item) { this.itens.add(item); }
    
    public void recalcularItens() {
        for (VendaItem item : itens) {
            item.calcularTotais();
        }
        calcularTotais();
    }
    
    public void calcularTotais() {
        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;

        for (VendaItem item : itens) {
            bruto = bruto.add(item.getTotalBruto());
            iva = iva.add(item.getTotalIva());
        }

        this.totalBruto = bruto;
        this.totalIva = iva;
        this.totalLiquido = bruto.add(iva);
    }
    
    public void addItemInteligente(Produto produto, BigDecimal quantidade) {

        VendaItem item = new VendaItem();
        item.setVenda(this);
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(produto.getPrecoBase());
        item.setTaxaIva(produto.isTemIva() ? produto.getTaxaIva() : BigDecimal.ZERO);

        // 🔥 Calcula o item completo
        item.calcularTotais();

        // adiciona à lista
        this.itens.add(item);

        // 🔥 Recalcula totais da venda
        this.calcularTotais();
    }
    
}