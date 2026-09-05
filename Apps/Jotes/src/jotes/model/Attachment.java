package jotes.model;

import java.time.Instant;

/** Ficheiro anexado a uma nota (guardado em data/attachments/). */
public class Attachment {
    private long id;
    private long noteId;
    private String filename = "";
    private String relpath = "";
    private long sizeBytes;
    private Instant createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename == null ? "" : filename; }

    /** Caminho relativo à raiz de attachments (ex.: "12/imagem.png"). */
    public String getRelpath() { return relpath; }
    public void setRelpath(String relpath) { this.relpath = relpath == null ? "" : relpath; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isImage() {
        String n = filename.toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                || n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".webp");
    }

    @Override
    public String toString() { return filename; }
}
