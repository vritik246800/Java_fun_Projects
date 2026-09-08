package app.modelo;

import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

/** Veículo de transporte público, animado ao longo da sua rota (ida e volta). */
public class Veiculo {

    private final int id;
    private final String matricula;
    private final String tipo;
    private final double velocidadeKmh;
    private Rota rota;

    // estado da animação
    private double distanciaKm; // distância percorrida ao longo da rota
    private int direcao = 1;    // 1 = ida, -1 = volta
    private GeoPosition posicao;

    public Veiculo(int id, String matricula, String tipo, Rota rota, double velocidadeKmh) {
        this.id = id;
        this.matricula = matricula;
        this.tipo = tipo;
        this.rota = rota;
        this.velocidadeKmh = velocidadeKmh;
        actualizarPosicao();
    }

    public int getId() {
        return id;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getTipo() {
        return tipo;
    }

    public double getVelocidadeKmh() {
        return velocidadeKmh;
    }

    public Rota getRota() {
        return rota;
    }

    public GeoPosition getPosicao() {
        return posicao;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
        this.distanciaKm = 0;
        this.direcao = 1;
        actualizarPosicao();
    }

    /** Comprimento total da rota do veículo, em km. */
    public double comprimentoRotaKm() {
        return rota == null ? 0 : rota.comprimentoKm();
    }

    /** Avança o veículo ao longo da rota; no fim inverte o sentido (ida e volta). */
    public void avancar(double km) {
        double comprimento = comprimentoRotaKm();
        if (comprimento <= 0) {
            actualizarPosicao(); // rota sem percurso: fica parado
            return;
        }
        distanciaKm += km * direcao;
        if (distanciaKm >= comprimento) {
            distanciaKm = comprimento;
            direcao = -1;
        } else if (distanciaKm <= 0) {
            distanciaKm = 0;
            direcao = 1;
        }
        actualizarPosicao();
    }

    private void actualizarPosicao() {
        if (rota == null || rota.getParagens().isEmpty()) {
            posicao = null;
            return;
        }
        List<Paragem> ps = rota.getParagens();
        if (ps.size() == 1) {
            posicao = ps.get(0).geo();
            return;
        }
        double restante = distanciaKm;
        for (int i = 1; i < ps.size(); i++) {
            Paragem a = ps.get(i - 1);
            Paragem b = ps.get(i);
            double seg = a.distanciaKm(b);
            if (restante <= seg) {
                double f = seg == 0 ? 0 : restante / seg;
                posicao = new GeoPosition(
                        a.getLatitude() + (b.getLatitude() - a.getLatitude()) * f,
                        a.getLongitude() + (b.getLongitude() - a.getLongitude()) * f);
                return;
            }
            restante -= seg;
        }
        posicao = ps.get(ps.size() - 1).geo();
    }

    @Override
    public String toString() {
        String r = rota != null ? rota.getNome() : "sem rota";
        return matricula + " · " + tipo + " · " + r;
    }
}
