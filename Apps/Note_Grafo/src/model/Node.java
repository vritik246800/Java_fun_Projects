package model;

/**
 * Persistable representation for a Node in the graph (data only).
 */
public class Node {
    public double x;
    public double y;
    public String label;
    public String note;
    public String colorHex; // e.g. "#66CCFF"

    public Node() {}

    public Node(double x, double y, String label, String note, String colorHex) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.note = note;
        this.colorHex = colorHex;
    }
}
