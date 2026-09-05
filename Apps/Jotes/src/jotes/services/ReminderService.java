package jotes.services;

import jotes.db.Database;
import jotes.model.Note;
import jotes.util.Log;

import javax.swing.Timer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lembretes com hora marcada. Os lembretes vivem na tabela {@code reminders} e um
 * {@link Timer} do Swing varre os que já venceram uma vez por minuto, entregando-os
 * na thread da UI — não há threads extra nem agendador externo a manter.
 *
 * <p>ponytail: varrimento periódico de um minuto em vez de agendamento por lembrete
 * (Quartz está no classpath). Um minuto de atraso máximo é aceitável para notas;
 * se for preciso ao segundo, trocar o varrimento por um agendamento por lembrete.</p>
 */
public class ReminderService {

    /** Intervalo do varrimento: um minuto é a granularidade dos lembretes. */
    private static final int SWEEP_MS = 60_000;

    /** Um lembrete agendado. */
    public record Reminder(long id, long noteId, Instant remindAt, String message, boolean fired) {}

    private final Database db;
    private final Timer sweeper;
    private Consumer<Reminder> onDue = r -> {};

    public ReminderService(Database db) {
        this.db = db;
        this.sweeper = new Timer(SWEEP_MS, e -> sweep());
        sweeper.setInitialDelay(3_000); // deixa a janela abrir antes do primeiro aviso
    }

    /** Chamado na thread da UI para cada lembrete vencido. */
    public void setOnDue(Consumer<Reminder> listener) {
        this.onDue = listener == null ? r -> {} : listener;
    }

    public void start() {
        sweeper.start();
    }

    public void stop() {
        sweeper.stop();
    }

    /** Agenda um lembrete para uma nota. */
    public Reminder add(long noteId, Instant when, String message) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO reminders(note_id, remind_at, message, fired) VALUES(?, ?, ?, 0)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, noteId);
            ps.setLong(2, when.toEpochMilli());
            ps.setString(3, message == null ? "" : message);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return new Reminder(rs.getLong(1), noteId, when, message, false);
            }
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM reminders WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Lembretes por disparar de uma nota, do mais próximo para o mais distante. */
    public List<Reminder> pendingFor(long noteId) throws SQLException {
        return query("SELECT * FROM reminders WHERE note_id = ? AND fired = 0 ORDER BY remind_at", noteId);
    }

    /** Todos os lembretes por disparar. */
    public List<Reminder> pending() throws SQLException {
        return query("SELECT * FROM reminders WHERE fired = 0 ORDER BY remind_at", null);
    }

    private List<Reminder> query(String sql, Long noteId) throws SQLException {
        List<Reminder> out = new ArrayList<>();
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            if (noteId != null) ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Reminder(rs.getLong("id"), rs.getLong("note_id"),
                            Instant.ofEpochMilli(rs.getLong("remind_at")),
                            rs.getString("message"), rs.getInt("fired") == 1));
                }
            }
        }
        return out;
    }

    /** Entrega os lembretes vencidos e marca-os como disparados. */
    private void sweep() {
        try {
            long now = System.currentTimeMillis();
            List<Reminder> due = new ArrayList<>();
            try (PreparedStatement ps = db.conn().prepareStatement(
                    "SELECT * FROM reminders WHERE fired = 0 AND remind_at <= ? ORDER BY remind_at")) {
                ps.setLong(1, now);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        due.add(new Reminder(rs.getLong("id"), rs.getLong("note_id"),
                                Instant.ofEpochMilli(rs.getLong("remind_at")),
                                rs.getString("message"), false));
                    }
                }
            }
            if (due.isEmpty()) return;
            try (PreparedStatement ps = db.conn().prepareStatement(
                    "UPDATE reminders SET fired = 1 WHERE id = ?")) {
                for (Reminder r : due) {
                    ps.setLong(1, r.id());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            // o Timer do Swing já corre na thread da UI: entrega direta
            for (Reminder r : due) onDue.accept(r);
        } catch (SQLException ex) {
            Log.warn(ReminderService.class, "Falha ao verificar lembretes", ex);
        }
    }

    /** Texto do aviso: a mensagem do lembrete ou, na falta dela, o título da nota. */
    public static String describe(Reminder reminder, Note note) {
        if (reminder.message() != null && !reminder.message().isBlank()) return reminder.message();
        return note == null ? "Lembrete" : "Lembrete: " + note.displayTitle();
    }
}
