package jotes.db;

import jotes.model.CanvasBoard;
import jotes.model.CanvasEdge;
import jotes.model.CanvasNode;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acesso às tabelas do canvas (quadros, cartões e ligações). */
public class CanvasRepository {

    private final Database db;

    public CanvasRepository(Database db) {
        this.db = db;
    }

    /** Pasta de dados (para guardar imagens do canvas). */
    public java.nio.file.Path dataDir() {
        return db.dir();
    }

    public List<CanvasBoard> listBoards() throws SQLException {
        List<CanvasBoard> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT * FROM canvases ORDER BY name COLLATE NOCASE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CanvasBoard b = new CanvasBoard();
                b.setId(rs.getLong("id"));
                b.setName(rs.getString("name"));
                out.add(b);
            }
        }
        return out;
    }

    public CanvasBoard createBoard(String name) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO canvases(name, created_at) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                CanvasBoard b = new CanvasBoard();
                b.setId(rs.getLong(1));
                b.setName(name);
                return b;
            }
        }
    }

    public void renameBoard(long id, String name) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("UPDATE canvases SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void deleteBoard(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM canvases WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<CanvasNode> nodes(long canvasId) throws SQLException {
        List<CanvasNode> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT * FROM canvas_nodes WHERE canvas_id = ? ORDER BY id")) {
            ps.setLong(1, canvasId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapNode(rs));
            }
        }
        return out;
    }

    public List<CanvasEdge> edges(long canvasId) throws SQLException {
        List<CanvasEdge> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT * FROM canvas_edges WHERE canvas_id = ? ORDER BY id")) {
            ps.setLong(1, canvasId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CanvasEdge e = new CanvasEdge();
                    e.setId(rs.getLong("id"));
                    e.setCanvasId(rs.getLong("canvas_id"));
                    e.setFromNode(rs.getLong("from_node"));
                    e.setToNode(rs.getLong("to_node"));
                    e.setLabel(rs.getString("label"));
                    e.setColor(rs.getString("color"));
                    e.setFromEnd(rs.getString("from_end"));
                    e.setToEnd(rs.getString("to_end"));
                    out.add(e);
                }
            }
        }
        return out;
    }

    public CanvasNode addNode(CanvasNode n) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO canvas_nodes(canvas_id, note_id, kind, text, x, y, w, h, color) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, n.getCanvasId());
            if (n.getNoteId() == null) ps.setNull(2, Types.INTEGER); else ps.setLong(2, n.getNoteId());
            ps.setString(3, n.getKind());
            ps.setString(4, n.getText());
            ps.setDouble(5, n.getX());
            ps.setDouble(6, n.getY());
            ps.setDouble(7, n.getW());
            ps.setDouble(8, n.getH());
            ps.setString(9, n.getColor());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                n.setId(rs.getLong(1));
            }
        }
        return n;
    }

    public void updateNode(CanvasNode n) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE canvas_nodes SET kind = ?, text = ?, x = ?, y = ?, w = ?, h = ?, color = ? WHERE id = ?")) {
            ps.setString(1, n.getKind());
            ps.setString(2, n.getText());
            ps.setDouble(3, n.getX());
            ps.setDouble(4, n.getY());
            ps.setDouble(5, n.getW());
            ps.setDouble(6, n.getH());
            ps.setString(7, n.getColor());
            ps.setLong(8, n.getId());
            ps.executeUpdate();
        }
    }

    public void deleteNode(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM canvas_nodes WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public CanvasEdge addEdge(long canvasId, long fromNode, long toNode) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO canvas_edges(canvas_id, from_node, to_node) VALUES(?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, canvasId);
            ps.setLong(2, fromNode);
            ps.setLong(3, toNode);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                CanvasEdge e = new CanvasEdge();
                e.setId(rs.getLong(1));
                e.setCanvasId(canvasId);
                e.setFromNode(fromNode);
                e.setToNode(toNode);
                return e;
            }
        }
    }

    public void updateEdge(CanvasEdge e) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE canvas_edges SET label = ?, color = ?, from_end = ?, to_end = ? WHERE id = ?")) {
            ps.setString(1, e.getLabel());
            ps.setString(2, e.getColor());
            ps.setString(3, e.getFromEnd());
            ps.setString(4, e.getToEnd());
            ps.setLong(5, e.getId());
            ps.executeUpdate();
        }
    }

    public void deleteEdge(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM canvas_edges WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static CanvasNode mapNode(ResultSet rs) throws SQLException {
        CanvasNode n = new CanvasNode();
        n.setId(rs.getLong("id"));
        n.setCanvasId(rs.getLong("canvas_id"));
        long noteId = rs.getLong("note_id");
        n.setNoteId(rs.wasNull() ? null : noteId);
        n.setKind(rs.getString("kind"));
        n.setText(rs.getString("text"));
        n.setX(rs.getDouble("x"));
        n.setY(rs.getDouble("y"));
        n.setW(rs.getDouble("w"));
        n.setH(rs.getDouble("h"));
        n.setColor(rs.getString("color"));
        return n;
    }
}
