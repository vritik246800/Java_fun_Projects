package jotes.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Note {
    private long id;
    private String uuid = "";
    private String title = "";
    private String content = "";
    private Long folderId;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean pinned;
    private boolean archived;
    private boolean locked;
    private Instant deletedAt;
    private List<String> tags = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid == null ? "" : uuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? "" : title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    /** Nota cifrada com senha: {@code content} guarda o blob em base64. */
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    /** Momento em que foi para a lixeira, ou {@code null} se estiver ativa. */
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public boolean isDeleted() { return deletedAt != null; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags == null ? new ArrayList<>() : tags; }

    public String displayTitle() {
        return title.isBlank() ? "Nova nota" : title;
    }

    public String snippet() {
        String s = content.replaceAll("\\s+", " ").trim();
        return s.length() > 80 ? s.substring(0, 80) : s;
    }
}
