package jotes.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Repositório de tags (normalizadas para minúsculas, sem '#'). */
public class TagRepository {
    private final Database db;

    public TagRepository(Database db) {
        this.db = db;
    }

    /** Normaliza uma tag: sem '#', sem espaços extra, minúsculas. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        while (t.startsWith("#")) t = t.substring(1);
        return t.trim().toLowerCase(Locale.ROOT);
    }

    /** Todas as tags em uso, ordenadas. */
    public List<String> listAll() throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT DISTINCT t.name FROM tags t JOIN note_tags nt ON nt.tag_id = t.id ORDER BY t.name COLLATE NOCASE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    public List<String> tagsOf(long noteId) throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT t.name FROM tags t JOIN note_tags nt ON nt.tag_id = t.id"
                        + " WHERE nt.note_id = ? ORDER BY t.name COLLATE NOCASE")) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        }
        return out;
    }

    /** Substitui as tags de uma nota. */
    public void setTags(long noteId, Collection<String> rawTags) throws SQLException {
        Set<String> names = new LinkedHashSet<>();
        for (String raw : rawTags) {
            String t = normalize(raw);
            if (!t.isEmpty()) names.add(t);
        }

        try (PreparedStatement del = db.conn().prepareStatement("DELETE FROM note_tags WHERE note_id = ?")) {
            del.setLong(1, noteId);
            del.executeUpdate();
        }
        try (PreparedStatement insTag = db.conn().prepareStatement("INSERT OR IGNORE INTO tags(name) VALUES(?)");
             PreparedStatement link = db.conn().prepareStatement(
                     "INSERT OR IGNORE INTO note_tags(note_id, tag_id)"
                             + " SELECT ?, id FROM tags WHERE name = ? COLLATE NOCASE")) {
            for (String name : names) {
                insTag.setString(1, name);
                insTag.executeUpdate();
                link.setLong(1, noteId);
                link.setString(2, name);
                link.executeUpdate();
            }
        }
        // limpa tags órfãs
        try (PreparedStatement clean = db.conn().prepareStatement(
                "DELETE FROM tags WHERE id NOT IN (SELECT tag_id FROM note_tags)")) {
            clean.executeUpdate();
        }
    }
}
