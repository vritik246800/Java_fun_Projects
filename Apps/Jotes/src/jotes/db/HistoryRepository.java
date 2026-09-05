package jotes.db;

import jotes.model.HistoryEntry;
import jotes.model.Note;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Histórico de versões das notas (snapshot coalescido por intervalo mínimo). */
public class HistoryRepository {
    /** Intervalo mínimo entre snapshots automáticos. */
    public static final long DEFAULT_MIN_INTERVAL_MS = 2 * 60 * 1000;
    /** Máximo de versões guardadas por nota. */
    public static final int MAX_PER_NOTE = 100;

    private final Database db;

    public HistoryRepository(Database db) {
        this.db = db;
    }

    /** Guarda um snapshot da nota se passou tempo suficiente desde o último e algo mudou. */
    public void maybeSnapshot(Note n) throws SQLException {
        maybeSnapshot(n, DEFAULT_MIN_INTERVAL_MS);
    }

    public void maybeSnapshot(Note n, long minIntervalMs) throws SQLException {
        HistoryEntry last = latest(n.getId());
        long now = System.currentTimeMillis();
        if (last != null) {
            boolean changed = !last.getTitle().equals(n.getTitle()) || !last.getContent().equals(n.getContent());
            boolean due = now - last.getSavedAt().toEpochMilli() >= minIntervalMs;
            if (!changed || !due) return;
        }
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO note_history(note_id, title, content, saved_at) VALUES(?, ?, ?, ?)")) {
            ps.setLong(1, n.getId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getContent());
            ps.setLong(4, now);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = db.conn().prepareStatement(
                "DELETE FROM note_history WHERE note_id = ? AND id NOT IN"
                        + " (SELECT id FROM note_history WHERE note_id = ? ORDER BY saved_at DESC LIMIT ?)")) {
            ps.setLong(1, n.getId());
            ps.setLong(2, n.getId());
            ps.setInt(3, MAX_PER_NOTE);
            ps.executeUpdate();
        }
    }

    public HistoryEntry latest(long noteId) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT * FROM note_history WHERE note_id = ? ORDER BY saved_at DESC LIMIT 1")) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Versões de uma nota, da mais recente para a mais antiga. */
    public List<HistoryEntry> list(long noteId) throws SQLException {
        List<HistoryEntry> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT * FROM note_history WHERE note_id = ? ORDER BY saved_at DESC LIMIT " + MAX_PER_NOTE)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public HistoryEntry find(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT * FROM note_history WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private HistoryEntry map(ResultSet rs) throws SQLException {
        HistoryEntry e = new HistoryEntry();
        e.setId(rs.getLong("id"));
        e.setNoteId(rs.getLong("note_id"));
        e.setTitle(rs.getString("title"));
        e.setContent(rs.getString("content"));
        e.setSavedAt(Instant.ofEpochMilli(rs.getLong("saved_at")));
        return e;
    }
}
