package app.modelo;

import org.jxmapviewer.viewer.GeoPosition;

/** Paragem de transporte público (ponto no mapa de Moçambique). */
public class Paragem {

    private static final double RAIO_TERRA_KM = 6371.0;

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

    /** Distância real (haversine) até outra paragem, em km. */
    public double distanciaKm(Paragem outra) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(outra.latitude);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(outra.longitude - longitude);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RAIO_TERRA_KM * 2 * Math.asin(Math.sqrt(h));
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
