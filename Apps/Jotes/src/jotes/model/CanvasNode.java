package jotes.model;

/** Cartão num canvas: texto livre, referência a nota, grupo ou imagem. */
public class CanvasNode {
    /** Tipos de cartão. */
    public static final String KIND_CARD = "card";
    public static final String KIND_NOTE = "note";
    public static final String KIND_GROUP = "group";
    public static final String KIND_IMAGE = "image";

    private long id;
    private long canvasId;
    private Long noteId;
    private String kind = KIND_CARD;
    private String text = "";
    private double x;
    private double y;
    private double w = 240;
    private double h = 130;
    private String color = "";

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCanvasId() { return canvasId; }
    public void setCanvasId(long canvasId) { this.canvasId = canvasId; }

    /** Nota associada; null para cartão de texto livre. */
    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    /** KIND_CARD, KIND_NOTE, KIND_GROUP ou KIND_IMAGE. */
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind == null ? KIND_CARD : kind; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text == null ? "" : text; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getW() { return w; }
    public void setW(double w) { this.w = w; }

    public double getH() { return h; }
    public void setH(double h) { this.h = h; }

    /** Nome da cor ("", "red", "orange", "yellow", "green", "blue", "purple"). */
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color == null ? "" : color; }

    public boolean isNote() { return KIND_NOTE.equals(kind) || noteId != null; }
    public boolean isGroup() { return KIND_GROUP.equals(kind); }
    public boolean isImage() { return KIND_IMAGE.equals(kind); }
}
