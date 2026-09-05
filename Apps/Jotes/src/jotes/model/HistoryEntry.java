package jotes.model;

import java.time.Instant;

/** Versão anterior de uma nota (histórico). */
public class HistoryEntry {
    private long id;
    private long noteId;
    private String title = "";
    private String content = "";
    private Instant savedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? "" : title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }
}
