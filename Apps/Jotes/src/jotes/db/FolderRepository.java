package jotes.db;

import jotes.model.Folder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FolderRepository {
    private final Database db;

    public FolderRepository(Database db) {
        this.db = db;
    }

    public List<Folder> list() throws SQLException {
        List<Folder> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT * FROM folders ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Folder(rs.getLong("id"), rs.getString("name")));
        }
        return out;
    }

    public Folder create(String name) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO folders(name) VALUES(?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return new Folder(rs.getLong(1), name);
            }
        }
    }

    public void rename(long id, String name) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("UPDATE folders SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM folders WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
