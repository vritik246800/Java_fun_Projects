package app.bd;

import app.modelo.Paragem;
import app.modelo.Rota;
import app.modelo.Veiculo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Acesso à base de dados SQLite (ficheiro transportes.db na pasta do projecto). */
public final class BaseDados {

    private static final String URL = "jdbc:sqlite:transportes.db";
    private static Connection conexao;

    private BaseDados() {
    }

    private static synchronized Connection conexao() throws SQLException {
        if (conexao == null || conexao.isClosed()) {
            conexao = DriverManager.getConnection(URL);
            try (Statement st = conexao.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }
        return conexao;
    }

    /** Cria as tabelas se não existirem e carrega dados iniciais se a base estiver vazia. */
    public static void inicializar() throws SQLException {
        try (Statement st = conexao().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS paragem ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "nome TEXT NOT NULL,"
                    + "latitude REAL NOT NULL,"
                    + "longitude REAL NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS rota ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "nome TEXT NOT NULL,"
                    + "cor TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS rota_paragem ("
                    + "rota_id INTEGER NOT NULL REFERENCES rota(id) ON DELETE CASCADE,"
                    + "paragem_id INTEGER NOT NULL REFERENCES paragem(id) ON DELETE CASCADE,"
                    + "ordem INTEGER NOT NULL,"
                    + "PRIMARY KEY (rota_id, paragem_id))");
            st.execute("CREATE TABLE IF NOT EXISTS veiculo ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "matricula TEXT NOT NULL UNIQUE,"
                    + "tipo TEXT NOT NULL,"
                    + "rota_id INTEGER REFERENCES rota(id) ON DELETE SET NULL,"
                    + "velocidade REAL NOT NULL DEFAULT 60)");
        }
        try (Statement st = conexao().createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM paragem")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // base já tem dados
            }
        }
        Connection c = conexao();
        boolean autoCommit = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            semear();
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(autoCommit);
        }
    }

    /** Dados iniciais: cidades reais de Moçambique e rotas pelas EN1/EN6. */
    private static void semear() throws SQLException {
        Paragem maputo = inserirParagem("Maputo", -25.9692, 32.5732);
        Paragem matola = inserirParagem("Matola", -25.9622, 32.4589);
        Paragem xaiXai = inserirParagem("Xai-Xai", -25.0519, 33.6442);
        Paragem chokwe = inserirParagem("Chokwé", -24.5333, 32.9833);
        Paragem maxixe = inserirParagem("Maxixe", -23.8597, 35.3472);
        Paragem inhambane = inserirParagem("Inhambane", -23.8650, 35.3833);
        Paragem vilanculos = inserirParagem("Vilanculos", -21.9986, 35.3139);
        Paragem inhassoro = inserirParagem("Inhassoro", -21.5333, 35.1833);
        Paragem beira = inserirParagem("Beira", -19.8436, 34.8389);
        Paragem dondo = inserirParagem("Dondo", -19.6094, 34.7431);
        Paragem chimoio = inserirParagem("Chimoio", -19.1164, 33.4833);
        Paragem gondola = inserirParagem("Gondola", -19.0850, 33.6630);
        Paragem moatize = inserirParagem("Moatize", -16.1028, 33.7297);
        Paragem tete = inserirParagem("Tete", -16.1564, 33.5867);
        Paragem nampula = inserirParagem("Nampula", -15.1165, 39.2666);
        Paragem nacala = inserirParagem("Nacala", -14.5427, 40.6728);
        Paragem pemba = inserirParagem("Pemba", -12.9740, 40.5178);

        Rota en1Sul = inserirRota("EN1 · Maputo–Beira", "#D32F2F");
        ligar(en1Sul.getId(), maputo, matola, xaiXai, chokwe, maxixe, inhambane, vilanculos, inhassoro, beira);

        Rota en6 = inserirRota("EN6 · Beira–Tete", "#1976D2");
        ligar(en6.getId(), beira, dondo, chimoio, gondola, moatize, tete);

        Rota en1Norte = inserirRota("EN1 Norte · Nampula–Pemba", "#388E3C");
        ligar(en1Norte.getId(), nampula, nacala, pemba);

        Rota circular = inserirRota("Circular · Maputo–Matola", "#F9A825");
        ligar(circular.getId(), maputo, matola);

        inserirVeiculo("ADF-4821", "Autocarro", en1Sul, 70);
        inserirVeiculo("MLB-7730", "Machimbombo", en6, 65);
        inserirVeiculo("NPL-2210", "Chapa", en1Norte, 80);
        inserirVeiculo("MAP-0100", "Chapa", circular, 45);
    }

    private static void ligar(int rotaId, Paragem... paragens) throws SQLException {
        for (Paragem p : paragens) {
            adicionarParagemARota(rotaId, p.getId());
        }
    }

    // ---------------- leitura ----------------

    public static List<Paragem> carregarParagens() throws SQLException {
        List<Paragem> lista = new ArrayList<>();
        try (Statement st = conexao().createStatement();
                ResultSet rs = st.executeQuery("SELECT id, nome, latitude, longitude FROM paragem ORDER BY nome")) {
            while (rs.next()) {
                lista.add(new Paragem(rs.getInt("id"), rs.getString("nome"),
                        rs.getDouble("latitude"), rs.getDouble("longitude")));
            }
        }
        return lista;
    }

    public static List<Rota> carregarRotas() throws SQLException {
        List<Rota> lista = new ArrayList<>();
        Map<Integer, Rota> porId = new HashMap<>();
        try (Statement st = conexao().createStatement();
                ResultSet rs = st.executeQuery("SELECT id, nome, cor FROM rota ORDER BY nome")) {
            while (rs.next()) {
                Rota rota = new Rota(rs.getInt("id"), rs.getString("nome"), Rota.corDeHex(rs.getString("cor")));
                lista.add(rota);
                porId.put(rota.getId(), rota);
            }
        }
        try (Statement st = conexao().createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT rp.rota_id, p.id, p.nome, p.latitude, p.longitude"
                        + " FROM rota_paragem rp JOIN paragem p ON p.id = rp.paragem_id"
                        + " ORDER BY rp.rota_id, rp.ordem")) {
            while (rs.next()) {
                Rota rota = porId.get(rs.getInt("rota_id"));
                if (rota != null) {
                    rota.getParagens().add(new Paragem(rs.getInt("id"), rs.getString("nome"),
                            rs.getDouble("latitude"), rs.getDouble("longitude")));
                }
            }
        }
        return lista;
    }

    public static List<Veiculo> carregarVeiculos(List<Rota> rotas) throws SQLException {
        Map<Integer, Rota> porId = new HashMap<>();
        for (Rota r : rotas) {
            porId.put(r.getId(), r);
        }
        List<Veiculo> lista = new ArrayList<>();
        try (Statement st = conexao().createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT id, matricula, tipo, rota_id, velocidade FROM veiculo ORDER BY matricula")) {
            while (rs.next()) {
                int rotaId = rs.getInt("rota_id");
                Rota rota = rs.wasNull() ? null : porId.get(rotaId);
                lista.add(new Veiculo(rs.getInt("id"), rs.getString("matricula"),
                        rs.getString("tipo"), rota, rs.getDouble("velocidade")));
            }
        }
        return lista;
    }

    // ---------------- escrita ----------------

    public static Paragem inserirParagem(String nome, double latitude, double longitude) throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement(
                "INSERT INTO paragem(nome, latitude, longitude) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.setDouble(2, latitude);
            ps.setDouble(3, longitude);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return new Paragem(rs.getInt(1), nome, latitude, longitude);
            }
        }
    }

    public static void apagarParagem(int id) throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement("DELETE FROM paragem WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static Rota inserirRota(String nome, String corHex) throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement(
                "INSERT INTO rota(nome, cor) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.setString(2, corHex);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return new Rota(rs.getInt(1), nome, Rota.corDeHex(corHex));
            }
        }
    }

    public static void apagarRota(int id) throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement("DELETE FROM rota WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Liga a paragem ao fim da rota (ordem = última + 1). */
    public static void adicionarParagemARota(int rotaId, int paragemId) throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement(
                "INSERT OR IGNORE INTO rota_paragem(rota_id, paragem_id, ordem)"
                + " VALUES (?,?, (SELECT COALESCE(MAX(ordem), 0) + 1 FROM rota_paragem WHERE rota_id = ?))")) {
            ps.setInt(1, rotaId);
            ps.setInt(2, paragemId);
            ps.setInt(3, rotaId);
            ps.executeUpdate();
        }
    }

    public static void removerParagemDaRota(int rotaId, int paragemId) throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement(
                "DELETE FROM rota_paragem WHERE rota_id = ? AND paragem_id = ?")) {
            ps.setInt(1, rotaId);
            ps.setInt(2, paragemId);
            ps.executeUpdate();
        }
    }

    public static Veiculo inserirVeiculo(String matricula, String tipo, Rota rota, double velocidade)
            throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement(
                "INSERT INTO veiculo(matricula, tipo, rota_id, velocidade) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, matricula);
            ps.setString(2, tipo);
            if (rota == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, rota.getId());
            }
            ps.setDouble(4, velocidade);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return new Veiculo(rs.getInt(1), matricula, tipo, rota, velocidade);
            }
        }
    }

    public static void apagarVeiculo(int id) throws SQLException {
        try (PreparedStatement ps = conexao().prepareStatement("DELETE FROM veiculo WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
