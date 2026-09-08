package app.rede;

import app.modelo.Paragem;
import app.modelo.Rota;
import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Resultado de um cálculo de percurso: as paragens por ordem e, para cada troço
     * entre duas paragens seguidas, as rotas que o servem.
     */
    public record Percurso(List<Paragem> paragens, List<List<Rota>> rotasPorTroco, double km) {

        /**
         * Mudanças de rota ao longo do percurso. Só conta quando nenhuma rota serve os
         * dois troços seguidos — troços partilhados por várias rotas não são transbordo.
         */
        public int transbordos() {
            int n = 0;
            for (int i = 1; i < rotasPorTroco.size(); i++) {
                if (Collections.disjoint(rotasPorTroco.get(i), rotasPorTroco.get(i - 1))) {
                    n++;
                }
            }
            return n;
        }

        /** Rota a mostrar num troço: mantém a do troço anterior sempre que também serve este. */
        public Rota rotaDoTroco(int i) {
            List<Rota> aqui = rotasPorTroco.get(i);
            if (aqui.isEmpty()) {
                return null;
            }
            if (i > 0) {
                Rota anterior = rotaDoTroco(i - 1);
                if (aqui.contains(anterior)) {
                    return anterior;
                }
            }
            return aqui.get(0);
        }

        /** Duração estimada, em minutos, à velocidade média indicada. */
        public int minutos(double velocidadeKmh) {
            return velocidadeKmh <= 0 ? 0 : (int) Math.round(km / velocidadeKmh * 60);
        }
    }

    private final SimpleWeightedGraph<Paragem, DefaultWeightedEdge> grafo =
            new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    private final Map<DefaultWeightedEdge, List<Rota>> rotasDaLigacao = new HashMap<>();

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
        List<List<Rota>> porTroco = new ArrayList<>();
        for (DefaultWeightedEdge ligacao : caminho.getEdgeList()) {
            porTroco.add(rotasDaLigacao.get(ligacao));
        }
        return new Percurso(caminho.getVertexList(), porTroco, caminho.getWeight());
    }

    private void ligar(Paragem a, Paragem b, Rota rota) {
        if (a.equals(b)) {
            return; // sem lacetes
        }
        DefaultWeightedEdge ligacao = grafo.getEdge(a, b);
        if (ligacao != null) {
            rotasDaLigacao.get(ligacao).add(rota); // troço partilhado por mais do que uma rota
            return;
        }
        ligacao = grafo.addEdge(a, b);
        grafo.setEdgeWeight(ligacao, a.distanciaKm(b));
        rotasDaLigacao.put(ligacao, new ArrayList<>(List.of(rota)));
    }
}
