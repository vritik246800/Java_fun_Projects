import java.util.ArrayList;
import java.util.Iterator;

/**
 * TAD GRAFO - IFRN- ESTRUTURA DE DADOS NÃO LINEARES - 2017.1
 * Versão modernizada com generics e ArrayList
 * 
 * @param <T> Tipo de dados que será armazenado nos vértices
 */
public class Grafo<T> {
    private int quantidadeVertices;
    private ArrayList<Vertice<T>> vertices;
    private ArrayList<Aresta>[][] matrizAdjacencia;

    @SuppressWarnings("unchecked")
    public Grafo() {
        this.quantidadeVertices = 0;
        this.vertices = new ArrayList<>();
        this.matrizAdjacencia = new ArrayList[0][0];
    }

    //====================================================================================
    //              REMOVER ARESTA
    //              remove uma aresta entre dois vertices (a primeira do arraylist)
    //-----------------------------------------------------------------------------------
    public void removerAresta(int chave1, int chave2) {
        int indice1 = encontraIndice(chave1), indice2 = encontraIndice(chave2);
        if (indice1 != -1 && indice2 != -1 && matrizAdjacencia[indice1][indice2] != null && !matrizAdjacencia[indice1][indice2].isEmpty()) {
            Aresta objetoRemovido = matrizAdjacencia[indice1][indice2].remove(0);
            if (matrizAdjacencia[indice2][indice1] != null) {
                matrizAdjacencia[indice2][indice1].remove(objetoRemovido);
            }
        }
    }

    //====================================================================================
    //              INSERIR ARESTA
    //              Insere uma aresta entre dois vertices
    //-----------------------------------------------------------------------------------
    public void inserirAresta(int chave1, int chave2) {
        int indice1 = encontraIndice(chave1), indice2 = encontraIndice(chave2);
        if (indice1 != -1 && indice2 != -1) {
            Aresta aresta = new Aresta();
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

    //====================================================================================
    //              INSERIR VERTICE
    //              Insere um vertice ao grafo, aumentando o tamanho da matriz
    //-----------------------------------------------------------------------------------
    public void inserirVertice(int chave, T valor) {
        if (encontraIndice(chave) == -1) {
            vertices.add(new Vertice<>(chave, valor));
            quantidadeVertices++;
            redimensionarMatriz();
        }
    }

    //====================================================================================
    //              REDIMENSIONAR MATRIZ
    //              Método auxiliar para redimensionar a matriz de adjacência
    //-----------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private void redimensionarMatriz() {
        ArrayList<Aresta>[][] tempMatrizAdjacencia = new ArrayList[quantidadeVertices][quantidadeVertices];
        
        // Copia elementos da matriz antiga para a nova
        for (int linha = 0; linha < quantidadeVertices - 1; linha++) {
            for (int coluna = 0; coluna < quantidadeVertices - 1; coluna++) {
                tempMatrizAdjacencia[linha][coluna] = matrizAdjacencia[linha][coluna];
            }
        }
        matrizAdjacencia = tempMatrizAdjacencia;
    }

    //====================================================================================
    //              REMOVER VERTICE
    //              Remove um vertice do grafo, reduzindo a matriz de adjacencia
    //------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void removerVertice(int chave) {
        int indice = encontraIndice(chave);
        if (indice != -1) {
            vertices.remove(indice);
            quantidadeVertices--;
            
            ArrayList<Aresta>[][] tempMatrizAdjacencia = new ArrayList[quantidadeVertices][quantidadeVertices];
            int controleLinha = 0;
            
            for (int linha = 0; linha <= quantidadeVertices; linha++) {
                if (linha != indice) {
                    int controleColuna = 0;
                    for (int coluna = 0; coluna <= quantidadeVertices; coluna++) {
                        if (coluna != indice) {
                            tempMatrizAdjacencia[controleLinha][controleColuna] = matrizAdjacencia[linha][coluna];
                            controleColuna++;
                        }
                    }
                    controleLinha++;
                }
            }
            matrizAdjacencia = tempMatrizAdjacencia;
        }
    }

    //====================================================================================
    //              ENCONTRA UM INDICE
    //              Busca o indice de uma chave no arraylist de vertices
    //------------------------------------------------------------------------------------
    private int encontraIndice(int chave) {
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).getChave() == chave) {
                return i;
            }
        }
        return -1;
    }

    //====================================================================================
    //              GET VERTICE
    //              Retorna o vértice pela chave
    //------------------------------------------------------------------------------------
    public Vertice<T> getVertice(int chave) {
        int indice = encontraIndice(chave);
        return indice != -1 ? vertices.get(indice) : null;
    }

    //====================================================================================
    //              GET TODOS VERTICES
    //              Retorna todos os vértices
    //------------------------------------------------------------------------------------
    public ArrayList<Vertice<T>> getTodosVertices() {
        return new ArrayList<>(vertices);
    }

    //====================================================================================
    //              PRINT GRAFO
    //              Exibe todos os vertices e matriz de adjacencia
    //------------------------------------------------------------------------------------
    public void printGrafo() {
        System.out.println("\n\nVertices: ([chave] valor)\n");
        for (Vertice<T> vertice : vertices) {
            System.out.println(vertice);
        }
        System.out.println("\n\nMatriz de Adjacencia:\n");
        for (ArrayList<Aresta>[] linha : matrizAdjacencia) {
            for (ArrayList<Aresta> coluna : linha) {
                System.out.print((coluna != null) ? "| " + coluna.size() + "\t" : "| 0\t");
            }
            System.out.println();
        }
    }

    //====================================================================================
    //              ORDEM
    //              Retorna a quantidade de vertices (Ordem)
    //------------------------------------------------------------------------------------
    public int ordem() {
        return quantidadeVertices;
    }

    //====================================================================================
    //              GRAU
    //              Retorna o grau de um vertices (Quantidades de arestas sobre o mesmo)
    //------------------------------------------------------------------------------------
    public int grau(int chave) {
        int indice = encontraIndice(chave);
        int grau = 0;
        
        if (indice != -1) {
            for (int coluna = 0; coluna < quantidadeVertices; coluna++) {
                if (matrizAdjacencia[indice][coluna] != null) {
                    for (Aresta aresta : matrizAdjacencia[indice][coluna]) {
                        if (aresta.isLoop()) {
                            grau += 2;
                        } else {
                            grau += 1;
                        }
                    }
                }
            }
        }
        return grau;
    }

    //====================================================================================
    //              CAMINHO EULERIANO
    //              Retorna TRUE se possuir um caminho euleriano
    //------------------------------------------------------------------------------------
    public boolean caminhoEuleriano() {
        int verticesImpar = 0;
        for (Vertice<T> vertice : vertices) {
            if (grau(vertice.getChave()) % 2 != 0) {
                verticesImpar++;
            }
        }
        return verticesImpar == 0 || verticesImpar == 2;
    }

    //====================================================================================
    //              CIRCUITO EULERIANO
    //              Retorna TRUE se possuir um circuito euleriano
    //------------------------------------------------------------------------------------
    public boolean circuitoEuleriano() {
        for (Vertice<T> vertice : vertices) {
            if (grau(vertice.getChave()) % 2 != 0) {
                return false;
            }
        }
        return true;
    }

    //====================================================================================
    //              EXISTE VERTICE
    //              Verifica se um vértice existe no grafo
    //------------------------------------------------------------------------------------
    public boolean existeVertice(int chave) {
        return encontraIndice(chave) != -1;
    }

    //====================================================================================
    //              EXISTE ARESTA
    //              Verifica se existe aresta entre dois vértices
    //------------------------------------------------------------------------------------
    public boolean existeAresta(int chave1, int chave2) {
        int indice1 = encontraIndice(chave1);
        int indice2 = encontraIndice(chave2);
        
        if (indice1 != -1 && indice2 != -1) {
            return matrizAdjacencia[indice1][indice2] != null && 
                   !matrizAdjacencia[indice1][indice2].isEmpty();
        }
        return false;
    }
}