import java.util.*;

// Classe auxiliar para representar uma aresta com peso e vértices
class ArestaKruskal implements Comparable<ArestaKruskal> {
    int origem, destino;
    double peso;
    
    public ArestaKruskal(int origem, int destino, double peso) {
        this.origem = origem;
        this.destino = destino;
        this.peso = peso;
    }
    
    @Override
    public int compareTo(ArestaKruskal outra) {
        return Double.compare(this.peso, outra.peso);
    }
    
    @Override
    public String toString() {
        return "(" + origem + " -> " + destino + ", peso: " + peso + ")";
    }
}

// Classe auxiliar para Union-Find (Disjoint Set)
class UnionFind {
    private int[] pai;
    private int[] rank;
    
    public UnionFind(int tamanho) {
        pai = new int[tamanho];
        rank = new int[tamanho];
        for (int i = 0; i < tamanho; i++) {
            pai[i] = i;
            rank[i] = 0;
        }
    }
    
    public int find(int x) {
        if (pai[x] != x) {
            pai[x] = find(pai[x]); // Compressão de caminho
        }
        return pai[x];
    }
    
    public boolean union(int x, int y) {
        int raizX = find(x);
        int raizY = find(y);
        
        if (raizX == raizY) return false; // Já estão no mesmo conjunto
        
        // Union por rank
        if (rank[raizX] < rank[raizY]) {
            pai[raizX] = raizY;
        } else if (rank[raizX] > rank[raizY]) {
            pai[raizY] = raizX;
        } else {
            pai[raizY] = raizX;
            rank[raizX]++;
        }
        return true;
    }
}

//====================================================================================
//              ALGORITMO DE KRUSKAL
//              Encontra a Árvore Geradora Mínima usando o algoritmo de Kruskal
//              ADICIONE ESTE MÉTODO À SUA CLASSE GRAFO<T>
//-----------------------------------------------------------------------------------
public ArrayList<ArestaKruskal> kruskal() {
    ArrayList<ArestaKruskal> resultado = new ArrayList<>();
    ArrayList<ArestaKruskal> todasArestas = new ArrayList<>();
    
    // Coletar todas as arestas do grafo
    for (int i = 0; i < quantidadeVertices; i++) {
        for (int j = i; j < quantidadeVertices; j++) { // j = i para evitar duplicatas
            if (matrizAdjacencia[i][j] != null && !matrizAdjacencia[i][j].isEmpty()) {
                int chaveOrigem = vertices.get(i).getChave();
                int chaveDestino = vertices.get(j).getChave();
                
                // Para cada aresta na posição (i,j)
                for (Aresta aresta : matrizAdjacencia[i][j]) {
                    if (!aresta.isLoop()) { // Ignorar loops
                        todasArestas.add(new ArestaKruskal(chaveOrigem, chaveDestino, aresta.getPeso()));
                    }
                }
            }
        }
    }
    
    // Ordenar arestas por peso (crescente)
    Collections.sort(todasArestas);
    
    // Criar estrutura Union-Find
    UnionFind uf = new UnionFind(quantidadeVertices * 10); // Tamanho suficiente para as chaves
    
    // Aplicar algoritmo de Kruskal
    for (ArestaKruskal aresta : todasArestas) {
        // Se os vértices não estão conectados, adicionar aresta
        if (uf.union(aresta.origem, aresta.destino)) {
            resultado.add(aresta);
            // Parar quando temos V-1 arestas (árvore completa)
            if (resultado.size() == quantidadeVertices - 1) {
                break;
            }
        }
    }
    
    return resultado;
}

//====================================================================================
//              PESO TOTAL DA MST
//              Calcula o peso total da Árvore Geradora Mínima
//-----------------------------------------------------------------------------------
public double pesoTotalMST() {
    ArrayList<ArestaKruskal> mst = kruskal();
    double pesoTotal = 0;
    for (ArestaKruskal aresta : mst) {
        pesoTotal += aresta.peso;
    }
    return pesoTotal;
}

//====================================================================================
//              PRINT MST
//              Exibe a Árvore Geradora Mínima
//-----------------------------------------------------------------------------------
public void printMST() {
    ArrayList<ArestaKruskal> mst = kruskal();
    System.out.println("\n=== ÁRVORE GERADORA MÍNIMA (Kruskal) ===");
    System.out.println("Arestas selecionadas:");
    
    double pesoTotal = 0;
    for (ArestaKruskal aresta : mst) {
        System.out.println("  " + aresta);
        pesoTotal += aresta.peso;
    }
    
    System.out.println("Peso total da MST: " + pesoTotal);
    System.out.println("Número de arestas: " + mst.size());
}

//====================================================================================
//              INSERIR ARESTA COM PESO
//              Versão do inserirAresta que permite especificar peso
//-----------------------------------------------------------------------------------
public void inserirAresta(int chave1, int chave2, double peso) {
    int indice1 = encontraIndice(chave1), indice2 = encontraIndice(chave2);
    if (indice1 != -1 && indice2 != -1) {
        Aresta aresta = new Aresta(peso); // Usar construtor com peso
        if(indice1 == indice2) {
            aresta.setLoop(true);
        } else {
            if (matrizAdjacencia[indice2][indice1] == null) {
                matrizAdjacencia[indice2][indice1] = new ArrayList<>();
            }
            matrizAdjacencia[indice2][indice1].add(aresta);
        }
        if (matrizAdjacencia[indice1][indice2] == null) {
            matrizAdjacencia[indice1][indice2] = new ArrayList<>();
        }
        matrizAdjacencia[indice1][indice2].add(aresta);
    }
}