package jotes.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Definições persistentes da aplicação (chave/valor). */
public class SettingsRepository {
    public static final String SYNC_FOLDER = "sync.folder";
    public static final String UI_DARK = "ui.dark";
    public static final String UI_REDUCE_ANIMATIONS = "ui.reduceAnimations";

    /** Posição dos divisores, em píxeis (memória dos splitters). */
    public static final String UI_SPLIT_SIDEBAR = "ui.split.sidebar";
    public static final String UI_SPLIT_LIST = "ui.split.list";
    /** Editor e pré-visualização lado a lado. */
    public static final String UI_SIDE_BY_SIDE = "ui.sideBySide";
    /** Geometria da janela principal: {@code x,y,largura,altura,maximizada}. */
    public static final String UI_WINDOW = "ui.window";

    /** Intervalo do backup automático em horas ({@code 0} desliga). */
    public static final String BACKUP_INTERVAL_HOURS = "backup.intervalHours";
    /** Instante (epoch ms) do último backup automático. */
    public static final String BACKUP_LAST_AT = "backup.lastAt";
    /** Quantos backups automáticos manter. */
    public static final String BACKUP_KEEP = "backup.keep";

    /** Modelo aplicado à nota diária. */
    public static final String DAILY_TEMPLATE = "editor.dailyTemplate";

    private final Database db;

    public SettingsRepository(Database db) {
        this.db = db;
    }

    public String get(String key, String defaultValue) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : defaultValue;
            }
        }
    }

    /** Valor inteiro da definição, ou {@code defaultValue} se faltar ou não for um número. */
    public int getInt(String key, int defaultValue) throws SQLException {
        String raw = get(key, null);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) throws SQLException {
        String raw = get(key, null);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) throws SQLException {
        String raw = get(key, null);
        return raw == null || raw.isBlank() ? defaultValue : Boolean.parseBoolean(raw.trim());
    }

    public void set(String key, int value) throws SQLException {
        set(key, Integer.toString(value));
    }

    public void set(String key, long value) throws SQLException {
        set(key, Long.toString(value));
    }

    public void set(String key, boolean value) throws SQLException {
        set(key, Boolean.toString(value));
    }

    public void set(String key, String value) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO settings(key, value) VALUES(?, ?)"
                        + " ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
