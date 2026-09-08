package app.rede;

import app.modelo.Paragem;
import app.modelo.Rota;
import java.awt.Color;
import java.util.List;

/**
 * Auto-teste da rede. Correr com as asserções ligadas:
 * {@code java -ea -cp "bin:lib/*" app.rede.TesteRede}
 */
public final class TesteRede {

    private TesteRede() {
    }

    public static void main(String[] args) {
        Paragem a = new Paragem(1, "A", -25.0, 32.0);
        Paragem b = new Paragem(2, "B", -24.0, 34.0);
        Paragem c = new Paragem(3, "C", -23.0, 32.0);
        Paragem d = new Paragem(4, "D", -20.0, 40.0); // fora de qualquer rota

        Rota desvio = new Rota(1, "Desvio", Color.RED); // A -> B -> C, mais longo
        desvio.getParagens().addAll(List.of(a, b, c));
        Rota directa = new Rota(2, "Directa", Color.BLUE); // A -> C
        directa.getParagens().addAll(List.of(a, c));

        RedeTransportes rede = new RedeTransportes(List.of(desvio, directa));
        RedeTransportes.Percurso p = rede.caminhoMaisCurto(a, c);

        assert p != null : "devia existir percurso A->C";
        assert p.paragens().equals(List.of(a, c)) : "Dijkstra devia usar a ligação directa: " + p.paragens();
        assert Math.abs(p.km() - a.distanciaKm(c)) < 0.001 : "peso do percurso errado: " + p.km();
        assert p.transbordos() == 0 : "percurso de uma só rota não tem transbordos";
        assert p.rotaDoTroco(0) == directa : "troço A-C é servido pela rota directa";
        assert p.minutos(60) == Math.round(p.km() / 60 * 60) : "estimativa de tempo errada";
        assert rede.caminhoMaisCurto(a, d) == null : "D não está ligada à rede";
        assert rede.caminhoMaisCurto(a, a) == null : "origem igual ao destino não é percurso";

        // troço partilhado por duas rotas não conta como transbordo
        Rota tambemAB = new Rota(3, "Alternativa", Color.GREEN);
        tambemAB.getParagens().addAll(List.of(a, b));
        RedeTransportes redeAB = new RedeTransportes(List.of(desvio, tambemAB));
        RedeTransportes.Percurso ac = redeAB.caminhoMaisCurto(a, c);
        assert ac != null && ac.paragens().size() == 3 : "A->C só pode ir por B";
        assert ac.transbordos() == 0 : "A-B serve as duas rotas, logo não há transbordo";

        System.out.printf("OK — A->C via %s: %.0f km%n", p.rotaDoTroco(0).getNome(), p.km());
    }
}
