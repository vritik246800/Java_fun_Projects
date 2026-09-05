package jotes.model;

/** Ligação dirigida entre dois cartões de um canvas. */
public class CanvasEdge {
    private long id;
    private long canvasId;
    private long fromNode;
    private long toNode;
    private String label = "";
    private String color = "";
    private String fromEnd = "none";  // "none" | "arrow"
    private String toEnd = "arrow";   // "none" | "arrow"

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCanvasId() { return canvasId; }
    public void setCanvasId(long canvasId) { this.canvasId = canvasId; }

    public long getFromNode() { return fromNode; }
    public void setFromNode(long fromNode) { this.fromNode = fromNode; }

    public long getToNode() { return toNode; }
    public void setToNode(long toNode) { this.toNode = toNode; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label == null ? "" : label; }

    /** Nome da cor ("", "red", "orange", "yellow", "green", "blue", "purple"). */
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color == null ? "" : color; }

    public String getFromEnd() { return fromEnd; }
    public void setFromEnd(String fromEnd) { this.fromEnd = "arrow".equals(fromEnd) ? "arrow" : "none"; }

    public String getToEnd() { return toEnd; }
    public void setToEnd(String toEnd) { this.toEnd = "arrow".equals(toEnd) ? "arrow" : "none"; }
}
