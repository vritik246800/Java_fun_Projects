package app.modelo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Rota de transporte: sequência ordenada de paragens, com cor própria no mapa. */
public class Rota {

    private final int id;
    private final String nome;
    private final Color cor;
    private final List<Paragem> paragens = new ArrayList<>();

    public Rota(int id, String nome, Color cor) {
        this.id = id;
        this.nome = nome;
        this.cor = cor;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Color getCor() {
        return cor;
    }

    /** Paragens da rota, por ordem de passagem. */
    public List<Paragem> getParagens() {
        return paragens;
    }

    public String corEmHex() {
        return String.format("#%02X%02X%02X", cor.getRed(), cor.getGreen(), cor.getBlue());
    }

    public static Color corDeHex(String hex) {
        try {
            return Color.decode(hex);
        } catch (RuntimeException e) {
            return Color.GRAY;
        }
    }

    @Override
    public String toString() {
        return nome;
    }
}
