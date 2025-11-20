package modelo;

public class Produto {

    private String nome;
    private double precoCusto;
    private double margemLucro;
    private boolean incluiIVA;
    private double precoFinal;
    private int stock;

    public Produto(String nome, double precoCusto, double margemLucro, boolean incluiIVA, int stock) {
        this.nome = nome;
        this.precoCusto = precoCusto;
        this.margemLucro = margemLucro;
        this.incluiIVA = incluiIVA;
        this.stock = stock;
        calcularPrecoFinal();
    }

    public void calcularPrecoFinal() {
        double valorMargem = precoCusto * (margemLucro / 100.0);
        double base = precoCusto + valorMargem;

        if (incluiIVA) {
            this.precoFinal = base * 1.16; // IVA 16%
        } else {
            this.precoFinal = base;
        }

        // Arredondar
        this.precoFinal = Math.round(this.precoFinal * 100.0) / 100.0;
    }

    // Getters
    public String getNome() { return nome; }
    public double getPrecoCusto() { return precoCusto; }
    public double getMargemLucro() { return margemLucro; }
    public boolean isIncluiIVA() { return incluiIVA; }
    public double getPrecoFinal() { return precoFinal; }
    public int getStock() { return stock; }

    @Override
    public String toString() {
        return nome;
    }
}
