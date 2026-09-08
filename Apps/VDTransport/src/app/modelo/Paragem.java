package app.modelo;

import org.jxmapviewer.viewer.GeoPosition;

/** Paragem de transporte público (ponto no mapa de Moçambique). */
public class Paragem {

    private final int id;
    private final String nome;
    private final double latitude;
    private final double longitude;

    public Paragem(int id, String nome, double latitude, double longitude) {
        this.id = id;
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public GeoPosition geo() {
        return new GeoPosition(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Paragem && ((Paragem) o).id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return nome;
    }
}
