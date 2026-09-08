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
        assert p.minutos(60) == Math.round(p.km() / 60 * 60) : "estimativa de tempo errada";
        assert rede.caminhoMaisCurto(a, d) == null : "D não está ligada à rede";
        assert rede.caminhoMaisCurto(a, a) == null : "origem igual ao destino não é percurso";
        assert rede.paragensLigadas(List.of(a, b, c, d)).equals(List.of(a, b, c)) : "D não pertence à rede";

        System.out.printf("OK — A->C via %s: %.0f km%n", p.rotas().get(0).getNome(), p.km());
    }
}
