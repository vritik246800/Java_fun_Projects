package app.sim;

import app.modelo.Veiculo;
import java.awt.Component;
import java.util.List;
import javax.swing.Timer;

/** Move os veículos ao longo das rotas num temporizador Swing e repinta o mapa. */
public class Simulador {

    private static final int INTERVALO_MS = 40;

    private final List<Veiculo> veiculos;
    private final Component mapa;
    private final Timer timer;
    private int escalaTempo = 1000; // 1 segundo real = N segundos simulados

    public Simulador(List<Veiculo> veiculos, Component mapa) {
        this.veiculos = veiculos;
        this.mapa = mapa;
        this.timer = new Timer(INTERVALO_MS, e -> passo());
    }

    private void passo() {
        double horasSimuladas = INTERVALO_MS / 1000.0 * escalaTempo / 3600.0;
        for (Veiculo v : veiculos) {
            v.avancar(v.getVelocidadeKmh() * horasSimuladas);
        }
        mapa.repaint();
    }

    public void iniciar() {
        timer.start();
    }

    public void parar() {
        timer.stop();
    }

    public boolean aDecorrer() {
        return timer.isRunning();
    }

    public int getEscalaTempo() {
        return escalaTempo;
    }

    public void setEscalaTempo(int escalaTempo) {
        this.escalaTempo = escalaTempo;
    }
}
