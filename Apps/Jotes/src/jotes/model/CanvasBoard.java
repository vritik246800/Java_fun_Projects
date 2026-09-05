package jotes.model;

/** Quadro de canvas (um "ficheiro" canvas ao estilo do Obsidian). */
public class CanvasBoard {
    private long id;
    private String name = "";

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "" : name; }

    @Override
    public String toString() { return name; }
}
