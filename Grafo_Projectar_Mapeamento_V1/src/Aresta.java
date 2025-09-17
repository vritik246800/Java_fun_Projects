/**
 * Classe que representa uma aresta no grafo
 */
public class Aresta {
    private boolean loop;
    private double peso; // Adicionado para suporte a grafos com peso
    
    public Aresta() {
        this.loop = false;
        this.peso = 1.0; // Peso padrão
    }
    
    public Aresta(double peso) {
        this.loop = false;
        this.peso = peso;
    }
    
    public boolean isLoop() {
        return loop;
    }
    
    public void setLoop(boolean loop) {
        this.loop = loop;
    }
    
    public double getPeso() {
        return peso;
    }
    
    public void setPeso(double peso) {
        this.peso = peso;
    }
    
    @Override
    public String toString() {
        return "Aresta{" +
               "loop=" + loop +
               ", peso=" + peso +
               '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Aresta aresta = (Aresta) obj;
        return loop == aresta.loop && Double.compare(aresta.peso, peso) == 0;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(loop, peso);
    }
}