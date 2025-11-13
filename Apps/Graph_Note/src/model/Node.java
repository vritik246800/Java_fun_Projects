package model;

/**
 * Persistable representation for a Node in the graph (data only).
 * The runtime Node used in the UI is in ui.GraphPanel (keeps visual fields).
 */
public class Node {
    public double x;
    public double y;
    public String label;
    public String note;

    // default constructor needed for Gson
    public Node() {}

    public Node(double x, double y, String label, String note) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.note = note;
    }
}
