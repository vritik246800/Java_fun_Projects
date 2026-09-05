package jotes.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Database implements AutoCloseable {
    private final Connection conn;
    private final Path file;
    private boolean ftsAvailable;

    public Database(Path file) throws Exception {
        this.file = file.toAbsolutePath();
        Files.createDirectories(this.file.getParent());
        conn = DriverManager.getConnection("jdbc:sqlite:" + this.file);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS folders(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notes(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL DEFAULT '',
                    content TEXT NOT NULL DEFAULT '',
                    folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    is_pinned INTEGER NOT NULL DEFAULT 0,
                    is_archived INTEGER NOT NULL DEFAULT 0
                )""");
        }
        migrate();
    }

    public Connection conn() { return conn; }

    /** Ficheiro da base de dados (caminho absoluto). */
    public Path file() { return file; }

    /** Pasta onde a base de dados vive (raiz para attachments, backups, etc.). */
    public Path dir() { return file.getParent(); }

    private void migrate() throws Exception {
        int version;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            version = rs.getInt(1);
        }

        if (version < 1) {
            // v1: uuid nas notas + tabelas de tags, links, anexos, histórico e definições
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE notes ADD COLUMN uuid TEXT");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tags(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE COLLATE NOCASE
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS note_tags(
                        note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                        tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                        PRIMARY KEY(note_id, tag_id)
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS note_links(
                        source_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                        target_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                        PRIMARY KEY(source_id, target_id)
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS attachments(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                        filename TEXT NOT NULL,
                        relpath TEXT NOT NULL,
                        size_bytes INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS note_history(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                        title TEXT NOT NULL DEFAULT '',
                        content TEXT NOT NULL DEFAULT '',
                        saved_at INTEGER NOT NULL
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS settings(
                        key TEXT PRIMARY KEY,
                        value TEXT
                    )""");
            }

            // Preenche uuid das notas existentes
            List<Long> missing = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM notes WHERE uuid IS NULL")) {
                while (rs.next()) missing.add(rs.getLong(1));
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE notes SET uuid = ? WHERE id = ?")) {
                for (Long id : missing) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_notes_uuid ON notes(uuid)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_note_history_note ON note_history(note_id, saved_at)");
                st.executeUpdate("PRAGMA user_version = 1");
            }
        }

        if (version < 2) {
            // v2: canvas estilo Obsidian (quadros, cartões e ligações)
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS canvases(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS canvas_nodes(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        canvas_id INTEGER NOT NULL REFERENCES canvases(id) ON DELETE CASCADE,
                        note_id INTEGER REFERENCES notes(id) ON DELETE CASCADE,
                        text TEXT NOT NULL DEFAULT '',
                        x REAL NOT NULL DEFAULT 0,
                        y REAL NOT NULL DEFAULT 0,
                        w REAL NOT NULL DEFAULT 240,
                        h REAL NOT NULL DEFAULT 130,
                        color TEXT NOT NULL DEFAULT ''
                    )""");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS canvas_edges(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        canvas_id INTEGER NOT NULL REFERENCES canvases(id) ON DELETE CASCADE,
                        from_node INTEGER NOT NULL REFERENCES canvas_nodes(id) ON DELETE CASCADE,
                        to_node INTEGER NOT NULL REFERENCES canvas_nodes(id) ON DELETE CASCADE
                    )""");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_canvas_nodes_canvas ON canvas_nodes(canvas_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_canvas_edges_canvas ON canvas_edges(canvas_id)");
                st.executeUpdate("PRAGMA user_version = 2");
            }
        }

        if (version < 3) {
            // v3: canvas com tipos de cartão (grupo/imagem) e arestas com etiqueta, cor e direção
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE canvas_nodes ADD COLUMN kind TEXT NOT NULL DEFAULT 'card'");
                st.executeUpdate("ALTER TABLE canvas_edges ADD COLUMN label TEXT NOT NULL DEFAULT ''");
                st.executeUpdate("ALTER TABLE canvas_edges ADD COLUMN color TEXT NOT NULL DEFAULT ''");
                st.executeUpdate("ALTER TABLE canvas_edges ADD COLUMN from_end TEXT NOT NULL DEFAULT 'none'");
                st.executeUpdate("ALTER TABLE canvas_edges ADD COLUMN to_end TEXT NOT NULL DEFAULT 'arrow'");
                st.executeUpdate("PRAGMA user_version = 3");
            }
        }

        if (version < 4) {
            // v4: lixeira (exclusão suave), bloqueio por senha e lembretes
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE notes ADD COLUMN deleted_at INTEGER");
                st.executeUpdate("ALTER TABLE notes ADD COLUMN is_locked INTEGER NOT NULL DEFAULT 0");
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS reminders(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                        remind_at INTEGER NOT NULL,
                        message TEXT NOT NULL DEFAULT '',
                        fired INTEGER NOT NULL DEFAULT 0
                    )""");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_deleted ON notes(deleted_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reminders_due ON reminders(fired, remind_at)");
                st.executeUpdate("PRAGMA user_version = 4");
            }
        }

        if (version < 5) {
            // v5: índice full-text (FTS5) sobre título e conteúdo, mantido por triggers.
            // FTS5 pode não existir na build do SQLite; nesse caso a pesquisa cai no LIKE.
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING fts5(
                        title, content, content='notes', content_rowid='id',
                        tokenize='unicode61 remove_diacritics 2')""");
                st.executeUpdate("""
                    CREATE TRIGGER IF NOT EXISTS notes_fts_ai AFTER INSERT ON notes BEGIN
                        INSERT INTO notes_fts(rowid, title, content) VALUES (new.id, new.title, new.content);
                    END""");
                st.executeUpdate("""
                    CREATE TRIGGER IF NOT EXISTS notes_fts_ad AFTER DELETE ON notes BEGIN
                        INSERT INTO notes_fts(notes_fts, rowid, title, content)
                        VALUES ('delete', old.id, old.title, old.content);
                    END""");
                st.executeUpdate("""
                    CREATE TRIGGER IF NOT EXISTS notes_fts_au AFTER UPDATE ON notes BEGIN
                        INSERT INTO notes_fts(notes_fts, rowid, title, content)
                        VALUES ('delete', old.id, old.title, old.content);
                        INSERT INTO notes_fts(rowid, title, content) VALUES (new.id, new.title, new.content);
                    END""");
                st.executeUpdate("INSERT INTO notes_fts(notes_fts) VALUES('rebuild')");
                ftsAvailable = true;
            } catch (SQLException e) {
                ftsAvailable = false;
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("PRAGMA user_version = 5");
            }
        } else {
            ftsAvailable = probeFts();
        }
    }

    /** Verifica se a tabela {@code notes_fts} existe e responde a consultas MATCH. */
    private boolean probeFts() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT rowid FROM notes_fts WHERE notes_fts MATCH 'jotes' LIMIT 1")) {
            rs.next();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** {@code true} se o índice FTS5 estiver disponível (pesquisa instantânea). */
    public boolean isFtsAvailable() {
        return ftsAvailable;
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) {}
    }
}
