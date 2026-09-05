package jotes.db;

import jotes.model.Note;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoteRepository {
    /** Ordenações suportadas em {@link #list(Long, boolean, boolean, String, String, String)}. */
    public static final String SORT_UPDATED = "updated";
    public static final String SORT_CREATED = "created";
    public static final String SORT_TITLE = "title";

    private final Database db;

    public NoteRepository(Database db) {
        this.db = db;
    }

    public Note create(Long folderId) throws SQLException {
        long now = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO notes(title, content, folder_id, created_at, updated_at, uuid) VALUES('', '', ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            if (folderId == null) ps.setNull(1, Types.INTEGER); else ps.setLong(1, folderId);
            ps.setLong(2, now);
            ps.setLong(3, now);
            ps.setString(4, uuid);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                Note n = new Note();
                n.setId(rs.getLong(1));
                n.setUuid(uuid);
                n.setFolderId(folderId);
                n.setCreatedAt(Instant.ofEpochMilli(now));
                n.setUpdatedAt(Instant.ofEpochMilli(now));
                return n;
            }
        }
    }

    public void save(Note n) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE notes SET title = ?, content = ?, folder_id = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, n.getTitle());
            ps.setString(2, n.getContent());
            if (n.getFolderId() == null) ps.setNull(3, Types.INTEGER); else ps.setLong(3, n.getFolderId());
            ps.setLong(4, now);
            ps.setLong(5, n.getId());
            ps.executeUpdate();
        }
        n.setUpdatedAt(Instant.ofEpochMilli(now));
    }

    public void updateFlags(Note n) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE notes SET is_pinned = ?, is_archived = ?, updated_at = ? WHERE id = ?")) {
            ps.setInt(1, n.isPinned() ? 1 : 0);
            ps.setInt(2, n.isArchived() ? 1 : 0);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, n.getId());
            ps.executeUpdate();
        }
    }

    public void move(long noteId, Long folderId) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE notes SET folder_id = ?, updated_at = ? WHERE id = ?")) {
            if (folderId == null) ps.setNull(1, Types.INTEGER); else ps.setLong(1, folderId);
            ps.setLong(2, System.currentTimeMillis());
            ps.setLong(3, noteId);
            ps.executeUpdate();
        }
    }

    /** Manda a nota para a lixeira (exclusão suave: continua na BD com {@code deleted_at}). */
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE notes SET deleted_at = ?, is_pinned = 0 WHERE id = ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Tira a nota da lixeira. */
    public void restore(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE notes SET deleted_at = NULL WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Apaga a nota da base de dados, sem retorno possível. */
    public void purge(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM notes WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Esvazia a lixeira. Devolve o número de notas apagadas definitivamente. */
    public int emptyTrash() throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "DELETE FROM notes WHERE deleted_at IS NOT NULL")) {
            return ps.executeUpdate();
        }
    }

    /** Marca a nota como cifrada (ou não) — o conteúdo é gerido por quem chama. */
    public void setLocked(long id, boolean locked) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE notes SET is_locked = ? WHERE id = ?")) {
            ps.setInt(1, locked ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public Note duplicate(Note src) throws SQLException {
        Note copy = create(src.getFolderId());
        copy.setTitle(src.getTitle().isBlank() ? "" : src.getTitle() + " (cópia)");
        copy.setContent(src.getContent());
        save(copy);
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT OR IGNORE INTO note_tags(note_id, tag_id) SELECT ?, tag_id FROM note_tags WHERE note_id = ?")) {
            ps.setLong(1, copy.getId());
            ps.setLong(2, src.getId());
            ps.executeUpdate();
        }
        return copy;
    }

    /**
     * Critérios de listagem. Exatamente uma vista está ativa: pasta (ou todas, com
     * {@code folderId == null}), fixadas, arquivadas ou lixeira; {@code tag} e
     * {@code text} são filtros adicionais e {@code limit}/{@code offset} paginam
     * (limit {@code <= 0} devolve tudo).
     */
    public record Query(Long folderId, boolean pinnedOnly, boolean archived, boolean trash,
                        String tag, String text, String sort, int limit, int offset) {

        public static Query all() {
            return new Query(null, false, false, false, null, null, SORT_UPDATED, 0, 0);
        }

        public Query withFolder(Long id) { return new Query(id, pinnedOnly, archived, trash, tag, text, sort, limit, offset); }
        public Query withPinnedOnly(boolean v) { return new Query(folderId, v, archived, trash, tag, text, sort, limit, offset); }
        public Query withArchived(boolean v) { return new Query(folderId, pinnedOnly, v, trash, tag, text, sort, limit, offset); }
        public Query withTrash(boolean v) { return new Query(folderId, pinnedOnly, archived, v, tag, text, sort, limit, offset); }
        public Query withTag(String v) { return new Query(folderId, pinnedOnly, archived, trash, v, text, sort, limit, offset); }
        public Query withText(String v) { return new Query(folderId, pinnedOnly, archived, trash, tag, v, sort, limit, offset); }
        public Query withSort(String v) { return new Query(folderId, pinnedOnly, archived, trash, tag, text, v, limit, offset); }
        public Query withPage(int limit, int offset) { return new Query(folderId, pinnedOnly, archived, trash, tag, text, sort, limit, offset); }

        boolean hasText() { return text != null && !text.isBlank(); }
        boolean hasTag() { return tag != null && !tag.isBlank(); }
    }

    /** Lista notas: fixadas primeiro, depois por data de modificação. */
    public List<Note> list(Long folderId, boolean pinnedOnly, boolean archived, String query) throws SQLException {
        return list(folderId, pinnedOnly, archived, null, query, SORT_UPDATED);
    }

    /**
     * Lista notas com filtros. A pesquisa cobre título, conteúdo, tags e nome da pasta.
     *
     * @param tag   se não for null/vazio, só notas com essa tag
     * @param sort  {@link #SORT_UPDATED}, {@link #SORT_CREATED} ou {@link #SORT_TITLE}
     */
    public List<Note> list(Long folderId, boolean pinnedOnly, boolean archived,
                           String tag, String query, String sort) throws SQLException {
        return list(Query.all().withFolder(folderId).withPinnedOnly(pinnedOnly)
                .withArchived(archived).withTag(tag).withText(query).withSort(sort));
    }

    /**
     * Lista notas segundo {@link Query}. Quando há texto e o índice FTS5 está disponível,
     * a pesquisa passa por {@code notes_fts}; caso contrário usa {@code LIKE} sobre
     * título, conteúdo, tags e nome da pasta.
     */
    public List<Note> list(Query q) throws SQLException {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT notes.* FROM notes WHERE ").append(where(q, args));
        sql.append(q.trash() ? " ORDER BY deleted_at DESC" : " ORDER BY is_pinned DESC, ");
        if (!q.trash()) {
            sql.append(switch (q.sort() == null ? SORT_UPDATED : q.sort()) {
                case SORT_CREATED -> "created_at DESC";
                case SORT_TITLE -> "title COLLATE NOCASE ASC";
                default -> "updated_at DESC";
            });
        }
        if (q.limit() > 0) {
            sql.append(" LIMIT ? OFFSET ?");
            args.add(q.limit());
            args.add(q.offset());
        }

        List<Note> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        loadTags(out);
        return out;
    }

    /** Número total de notas que satisfazem a query (ignora limit/offset). */
    public int count(Query q) throws SQLException {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM notes WHERE " + where(q, args);
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Constrói a cláusula WHERE partilhada por {@link #list(Query)} e {@link #count(Query)}. */
    private String where(Query q, List<Object> args) {
        StringBuilder sql = new StringBuilder();
        sql.append(q.trash() ? "deleted_at IS NOT NULL" : "deleted_at IS NULL AND is_archived = ?");
        if (!q.trash()) args.add(q.archived() ? 1 : 0);
        if (q.pinnedOnly()) sql.append(" AND is_pinned = 1");
        if (q.folderId() != null) {
            sql.append(" AND folder_id = ?");
            args.add(q.folderId());
        }
        if (q.hasTag()) {
            sql.append("""
                 AND EXISTS (SELECT 1 FROM note_tags nt JOIN tags t ON t.id = nt.tag_id
                             WHERE nt.note_id = notes.id AND t.name = ? COLLATE NOCASE)""");
            args.add(q.tag().trim());
        }
        if (q.hasText()) {
            if (useFts(q.text())) {
                sql.append(" AND notes.id IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH ?)");
                args.add(ftsQuery(q.text()));
            } else {
                sql.append("""
                     AND (title LIKE ? OR content LIKE ?
                          OR EXISTS (SELECT 1 FROM note_tags nt JOIN tags t ON t.id = nt.tag_id
                                     WHERE nt.note_id = notes.id AND t.name LIKE ?)
                          OR EXISTS (SELECT 1 FROM folders f
                                     WHERE f.id = notes.folder_id AND f.name LIKE ?))""");
                String like = "%" + q.text().trim() + "%";
                for (int i = 0; i < 4; i++) args.add(like);
            }
        }
        return sql.toString();
    }

    /** FTS só entra quando existe índice e a pesquisa não é uma expressão trivial. */
    private boolean useFts(String text) {
        return db.isFtsAvailable() && !ftsQuery(text).isBlank();
    }

    /**
     * Converte texto livre numa expressão FTS5 segura: cada palavra vira um termo
     * entre aspas com prefixo {@code *}, unidos por AND. Aspas do utilizador são
     * duplicadas para não quebrarem a sintaxe.
     */
    static String ftsQuery(String text) {
        StringBuilder sb = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String clean = word.replaceAll("[^\\p{L}\\p{N}_]", " ").trim();
            if (clean.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" AND ");
            sb.append('"').append(clean.replace("\"", "\"\"")).append("\"*");
        }
        return sb.toString();
    }

    /** Todas as notas (inclui arquivadas) — usado por export/backup/sync. */
    public List<Note> listAll() throws SQLException {
        List<Note> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT * FROM notes ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        loadTags(out);
        return out;
    }

    public Note findById(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT * FROM notes WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Note n = map(rs);
                loadTags(List.of(n));
                return n;
            }
        }
    }

    public Note findByUuid(String uuid) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT * FROM notes WHERE uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Procura por título exato (ignora maiúsculas), para resolver links [[título]]. */
    public Note findByTitle(String title) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT * FROM notes WHERE title = ? COLLATE NOCASE ORDER BY is_archived, updated_at DESC LIMIT 1")) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Insere ou atualiza uma nota vinda de sync/import, casando por uuid.
     * Em conflito ganha o updated_at mais recente. Devolve a nota local resultante.
     */
    public Note upsertFromSync(Note incoming) throws SQLException {
        Note existing = findByUuid(incoming.getUuid());
        if (existing == null) {
            try (PreparedStatement ps = db.conn().prepareStatement(
                    "INSERT INTO notes(title, content, folder_id, created_at, updated_at, is_pinned, is_archived, uuid)"
                            + " VALUES(?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, incoming.getTitle());
                ps.setString(2, incoming.getContent());
                if (incoming.getFolderId() == null) ps.setNull(3, Types.INTEGER);
                else ps.setLong(3, incoming.getFolderId());
                ps.setLong(4, incoming.getCreatedAt() != null ? incoming.getCreatedAt().toEpochMilli() : System.currentTimeMillis());
                ps.setLong(5, incoming.getUpdatedAt() != null ? incoming.getUpdatedAt().toEpochMilli() : System.currentTimeMillis());
                ps.setInt(6, incoming.isPinned() ? 1 : 0);
                ps.setInt(7, incoming.isArchived() ? 1 : 0);
                ps.setString(8, incoming.getUuid());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    incoming.setId(rs.getLong(1));
                }
            }
            return incoming;
        }
        long inUpd = incoming.getUpdatedAt() != null ? incoming.getUpdatedAt().toEpochMilli() : 0;
        long exUpd = existing.getUpdatedAt() != null ? existing.getUpdatedAt().toEpochMilli() : 0;
        if (inUpd > exUpd) {
            try (PreparedStatement ps = db.conn().prepareStatement(
                    "UPDATE notes SET title = ?, content = ?, folder_id = ?, updated_at = ?, is_pinned = ?, is_archived = ?"
                            + " WHERE id = ?")) {
                ps.setString(1, incoming.getTitle());
                ps.setString(2, incoming.getContent());
                if (incoming.getFolderId() == null) ps.setNull(3, Types.INTEGER);
                else ps.setLong(3, incoming.getFolderId());
                ps.setLong(4, inUpd);
                ps.setInt(5, incoming.isPinned() ? 1 : 0);
                ps.setInt(6, incoming.isArchived() ? 1 : 0);
                ps.setLong(7, existing.getId());
                ps.executeUpdate();
            }
            incoming.setId(existing.getId());
            return incoming;
        }
        return existing;
    }

    /** Preenche as tags de um conjunto de notas com uma única query. */
    public void loadTags(Collection<Note> notes) throws SQLException {
        if (notes.isEmpty()) return;
        Map<Long, Note> byId = new HashMap<>();
        for (Note n : notes) byId.put(n.getId(), n);

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) in.append(i == 0 ? "?" : ",?");
        String sql = "SELECT nt.note_id, t.name FROM note_tags nt JOIN tags t ON t.id = nt.tag_id"
                + " WHERE nt.note_id IN (" + in + ") ORDER BY t.name COLLATE NOCASE";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            int i = 1;
            for (Long id : byId.keySet()) ps.setLong(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note n = byId.get(rs.getLong("note_id"));
                    if (n != null) n.getTags().add(rs.getString("name"));
                }
            }
        }
    }

    public static Note map(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        long fid = rs.getLong("folder_id");
        n.setFolderId(rs.wasNull() ? null : fid);
        n.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        n.setUpdatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")));
        n.setPinned(rs.getInt("is_pinned") == 1);
        n.setArchived(rs.getInt("is_archived") == 1);
        try {
            n.setUuid(rs.getString("uuid"));
        } catch (SQLException ignored) {
            // coluna uuid pode não existir em resultados parciais
        }
        try {
            long del = rs.getLong("deleted_at");
            n.setDeletedAt(rs.wasNull() ? null : Instant.ofEpochMilli(del));
            n.setLocked(rs.getInt("is_locked") == 1);
        } catch (SQLException ignored) {
            // colunas da v4 podem não existir em resultados parciais
        }
        return n;
    }
}
