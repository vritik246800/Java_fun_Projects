package app.rede;

import app.modelo.Paragem;
import app.modelo.Rota;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * Rede de paragens ligadas pelas rotas. Calcula o percurso mais curto entre duas
 * paragens com o algoritmo de Dijkstra (JGraphT), pesando cada ligação pela
 * distância real em km.
 */
public final class RedeTransportes {

    /** Resultado de um cálculo de percurso: paragens por ordem e rota usada em cada troço. */
    public record Percurso(List<Paragem> paragens, List<Rota> rotas, double km) {

        /** Mudanças de rota ao longo do percurso. */
        public int transbordos() {
            int n = 0;
            for (int i = 1; i < rotas.size(); i++) {
                if (rotas.get(i) != rotas.get(i - 1)) {
                    n++;
                }
            }
            return n;
        }

        /** Duração estimada, em minutos, à velocidade média indicada. */
        public int minutos(double velocidadeKmh) {
            return velocidadeKmh <= 0 ? 0 : (int) Math.round(km / velocidadeKmh * 60);
        }
    }

    private final SimpleWeightedGraph<Paragem, DefaultWeightedEdge> grafo =
            new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    private final Map<DefaultWeightedEdge, Rota> rotaDaLigacao = new HashMap<>();

    public RedeTransportes(List<Rota> rotas) {
        for (Rota rota : rotas) {
            List<Paragem> ps = rota.getParagens();
            for (Paragem p : ps) {
                grafo.addVertex(p);
            }
            for (int i = 1; i < ps.size(); i++) {
                ligar(ps.get(i - 1), ps.get(i), rota);
            }
        }
    }

    /** Paragens que fazem parte da rede (pertencem a pelo menos uma rota). */
    public List<Paragem> paragensLigadas(List<Paragem> candidatas) {
        List<Paragem> lista = new ArrayList<>();
        for (Paragem p : candidatas) {
            if (grafo.containsVertex(p)) {
                lista.add(p);
            }
        }
        return lista;
    }

    /** Percurso mais curto entre duas paragens, ou null se não existir ligação. */
    public Percurso caminhoMaisCurto(Paragem origem, Paragem destino) {
        if (origem == null || destino == null || origem.equals(destino)
                || !grafo.containsVertex(origem) || !grafo.containsVertex(destino)) {
            return null;
        }
        GraphPath<Paragem, DefaultWeightedEdge> caminho =
                new DijkstraShortestPath<>(grafo).getPath(origem, destino);
        if (caminho == null) {
            return null;
        }
        List<Rota> usadas = new ArrayList<>();
        for (DefaultWeightedEdge ligacao : caminho.getEdgeList()) {
            usadas.add(rotaDaLigacao.get(ligacao));
        }
        return new Percurso(caminho.getVertexList(), usadas, caminho.getWeight());
    }

    private void ligar(Paragem a, Paragem b, Rota rota) {
        if (a.equals(b) || grafo.containsEdge(a, b)) {
            return; // sem lacetes nem ligações repetidas (a distância seria a mesma)
        }
        DefaultWeightedEdge ligacao = grafo.addEdge(a, b);
        grafo.setEdgeWeight(ligacao, a.distanciaKm(b));
        rotaDaLigacao.put(ligacao, rota);
    }
}
