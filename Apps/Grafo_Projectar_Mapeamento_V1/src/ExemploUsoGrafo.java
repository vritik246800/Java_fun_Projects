/**
 * Exemplos de uso do Grafo genérico com diferentes tipos de dados
 */
public class ExemploUsoGrafo {
    
    public static void main(String[] args) {
        exemploGrafoString();
        System.out.println("\n" + "=".repeat(50) + "\n");
        exemploGrafoInteger();
        System.out.println("\n" + "=".repeat(50) + "\n");
        exemploGrafoPessoa();
    }
    
    /**
     * Exemplo usando String como tipo de dados
     */
    public static void exemploGrafoString() {
        System.out.println("EXEMPLO: Grafo com Strings (Cidades)");
        
        Grafo<String> grafoCidades = new Grafo<>();
        
        // Inserindo vértices (cidades)
        grafoCidades.inserirVertice(1, "São Paulo");
        grafoCidades.inserirVertice(2, "Rio de Janeiro");
        grafoCidades.inserirVertice(3, "Brasília");
        grafoCidades.inserirVertice(4, "Salvador");
        
        // Inserindo arestas (conexões entre cidades)
        grafoCidades.inserirAresta(1, 2); // SP - RJ
        grafoCidades.inserirAresta(1, 3); // SP - Brasília
        grafoCidades.inserirAresta(2, 4); // RJ - Salvador
        grafoCidades.inserirAresta(3, 4); // Brasília - Salvador
        
        grafoCidades.printGrafo();
        
        System.out.println("Ordem do grafo: " + grafoCidades.ordem());
        System.out.println("Grau de São Paulo (chave 1): " + grafoCidades.grau(1));
        System.out.println("Possui caminho euleriano: " + grafoCidades.caminhoEuleriano());
        System.out.println("Possui circuito euleriano: " + grafoCidades.circuitoEuleriano());
    }
    
    /**
     * Exemplo usando Integer como tipo de dados
     */
    public static void exemploGrafoInteger() {
        System.out.println("EXEMPLO: Grafo com Integers (Valores numéricos)");
        
        Grafo<Integer> grafoNumeros = new Grafo<>();
        
        // Inserindo vértices com valores numéricos
        grafoNumeros.inserirVertice(10, 100);
        grafoNumeros.inserirVertice(20, 200);
        grafoNumeros.inserirVertice(30, 300);
        
        // Inserindo arestas
        grafoNumeros.inserirAresta(10, 20);
        grafoNumeros.inserirAresta(20, 30);
        grafoNumeros.inserirAresta(30, 10);
        
        grafoNumeros.printGrafo();
        
        System.out.println("Vértice com chave 20: " + grafoNumeros.getVertice(20));
        System.out.println("Existe aresta entre 10 e 20: " + grafoNumeros.existeAresta(10, 20));
    }
    
    /**
     * Exemplo usando uma classe personalizada
     */
    public static void exemploGrafoPessoa() {
        System.out.println("EXEMPLO: Grafo com objetos Pessoa");
        
        Grafo<Pessoa> grafoRede = new Grafo<>();
        
        // Inserindo vértices com objetos Pessoa
        grafoRede.inserirVertice(1, new Pessoa("Alice", 25));
        grafoRede.inserirVertice(2, new Pessoa("Bob", 30));
        grafoRede.inserirVertice(3, new Pessoa("Charlie", 28));
        grafoRede.inserirVertice(4, new Pessoa("Diana", 32));
        
        // Criando conexões na rede social
        grafoRede.inserirAresta(1, 2); // Alice conhece Bob
        grafoRede.inserirAresta(1, 3); // Alice conhece Charlie
        grafoRede.inserirAresta(2, 3); // Bob conhece Charlie
        grafoRede.inserirAresta(2, 4); // Bob conhece Diana
        grafoRede.inserirAresta(3, 4); // Charlie conhece Diana
        
        grafoRede.printGrafo();
        
        // Testando funcionalidades
        System.out.println("Pessoa na chave 1: " + grafoRede.getVertice(1).getValor());
        System.out.println("Alice tem " + grafoRede.grau(1) + " conexões");
        
        // Removendo uma pessoa da rede
        System.out.println("\nRemovendo Charlie (chave 3)...");
        grafoRede.removerVertice(3);
        grafoRede.printGrafo();
    }
}

/**
 * Classe auxiliar para demonstrar uso com objetos personalizados
 */
class Pessoa {
    private String nome;
    private int idade;
    
    public Pessoa(String nome, int idade) {
        this.nome = nome;
        this.idade = idade;
    }
    
    public String getNome() {
        return nome;
    }
    
    public int getIdade() {
        return idade;
    }
    
    @Override
    public String toString() {
        return nome + " (" + idade + " anos)";
    }
}