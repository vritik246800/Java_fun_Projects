/**
 * Classe Vertice genérica que pode armazenar qualquer tipo de dados
 * 
 * @param <T> Tipo de dados que será armazenado no vértice
 */
public class Vertice<T> {
    private int chave;
    private T valor;
    
    public Vertice(int chave, T valor) {
        this.chave = chave;
        this.valor = valor;
    }
    
    @Override
    public String toString() {
        return "[ " + chave + " ] " + valor;
    }
    
    public int getChave() {
        return chave;
    }

    public void setChave(int chave) {
        this.chave = chave;
    }

    public T getValor() {
        return valor;
    }

    public void setValor(T valor) {
        this.valor = valor;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Vertice<?> vertice = (Vertice<?>) obj;
        return chave == vertice.chave;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(chave);
    }
}