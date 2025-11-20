package modelo;

public class Cliente {

    private String nome;
    private String nif;
    private String tipo; // Individual / Empresa

    public Cliente(String nome, String nif, String tipo) {
        this.nome = nome;
        this.nif = nif;
        this.tipo = tipo;
    }

    public String getNome() { return nome; }
    public String getNif() { return nif; }
    public String getTipo() { return tipo; }

    @Override
    public String toString() {
        return nome + " (" + tipo + ")";
    }
}
