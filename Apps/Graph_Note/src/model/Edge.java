package model;

/**
 * Persistable representation for an edge: stores indices of nodes (a,b).
 */
public class Edge {
    public int a;
    public int b;

    public Edge() {}

    public Edge(int a, int b) {
        this.a = a;
        this.b = b;
    }
}
