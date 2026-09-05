package jotes.db;

import jotes.model.Attachment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AttachmentRepository {
    private final Database db;

    public AttachmentRepository(Database db) {
        this.db = db;
    }

    public Attachment add(long noteId, String filename, String relpath, long sizeBytes) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO attachments(note_id, filename, relpath, size_bytes, created_at) VALUES(?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, noteId);
            ps.setString(2, filename);
            ps.setString(3, relpath);
            ps.setLong(4, sizeBytes);
            ps.setLong(5, now);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                Attachment a = new Attachment();
                a.setId(rs.getLong(1));
                a.setNoteId(noteId);
                a.setFilename(filename);
                a.setRelpath(relpath);
                a.setSizeBytes(sizeBytes);
                a.setCreatedAt(Instant.ofEpochMilli(now));
                return a;
            }
        }
    }

    public List<Attachment> listForNote(long noteId) throws SQLException {
        List<Attachment> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at")) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public Attachment find(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT * FROM attachments WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM attachments WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Attachment map(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setNoteId(rs.getLong("note_id"));
        a.setFilename(rs.getString("filename"));
        a.setRelpath(rs.getString("relpath"));
        a.setSizeBytes(rs.getLong("size_bytes"));
        a.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        return a;
    }
}
