package jotes.db;

import jotes.model.Note;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Links [[título]] entre notas e backlinks. */
public class LinkRepository {
    private final Database db;

    public LinkRepository(Database db) {
        this.db = db;
    }

    /** Recria os links de uma nota a partir dos títulos encontrados no conteúdo. */
    public void syncFromTitles(long sourceId, Collection<String> titles) throws SQLException {
        Set<Long> targets = new LinkedHashSet<>();
        try (PreparedStatement find = db.conn().prepareStatement(
                "SELECT id FROM notes WHERE title = ? COLLATE NOCASE ORDER BY is_archived, updated_at DESC LIMIT 1")) {
            for (String title : titles) {
                if (title == null || title.isBlank()) continue;
                find.setString(1, title.trim());
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        if (id != sourceId) targets.add(id);
                    }
                }
            }
        }
        syncTargets(sourceId, targets);
    }

    /** Recria os links de saída de uma nota. */
    public void syncTargets(long sourceId, Collection<Long> targetIds) throws SQLException {
        try (PreparedStatement del = db.conn().prepareStatement("DELETE FROM note_links WHERE source_id = ?")) {
            del.setLong(1, sourceId);
            del.executeUpdate();
        }
        try (PreparedStatement ins = db.conn().prepareStatement(
                "INSERT OR IGNORE INTO note_links(source_id, target_id) VALUES(?, ?)")) {
            for (Long target : targetIds) {
                if (target == null || target == sourceId) continue;
                ins.setLong(1, sourceId);
                ins.setLong(2, target);
                ins.executeUpdate();
            }
        }
    }

    /** Notas para as quais esta nota aponta. */
    public List<Note> targetsOf(long sourceId) throws SQLException {
        return queryNotes("SELECT n.* FROM notes n JOIN note_links l ON l.target_id = n.id"
                + " WHERE l.source_id = ? ORDER BY n.title COLLATE NOCASE", sourceId);
    }

    /** Notas que apontam para esta nota (backlinks). */
    public List<Note> backlinksOf(long noteId) throws SQLException {
        return queryNotes("SELECT n.* FROM notes n JOIN note_links l ON l.source_id = n.id"
                + " WHERE l.target_id = ? ORDER BY n.updated_at DESC", noteId);
    }

    /** Todos os pares (source_id, target_id) — usado pelo graph view. */
    public List<long[]> allLinks() throws SQLException {
        List<long[]> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT source_id, target_id FROM note_links");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new long[]{rs.getLong(1), rs.getLong(2)});
        }
        return out;
    }

    private List<Note> queryNotes(String sql, long id) throws SQLException {
        List<Note> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(NoteRepository.map(rs));
            }
        }
        return out;
    }
}
