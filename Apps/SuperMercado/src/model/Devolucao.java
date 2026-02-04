package model;

import enums.TipoDevolucao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Devolucao {

    private Integer id;
    private Venda vendaOriginal;
    private Caixa caixa;
    private LocalDateTime dataHora;
    private TipoDevolucao tipo;
    private String motivo;

    private List<DevolucaoItem> itens = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Venda getVendaOriginal() { return vendaOriginal; }
    public void setVendaOriginal(Venda vendaOriginal) { this.vendaOriginal = vendaOriginal; }

    public Caixa getCaixa() { return caixa; }
    public void setCaixa(Caixa caixa) { this.caixa = caixa; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public TipoDevolucao getTipo() { return tipo; }
    public void setTipo(TipoDevolucao tipo) { this.tipo = tipo; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public List<DevolucaoItem> getItens() { return itens; }
    public void addItem(DevolucaoItem item) { this.itens.add(item); }
}