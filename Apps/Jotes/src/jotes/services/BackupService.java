package jotes.services;

import jotes.db.Database;
import jotes.db.SettingsRepository;
import jotes.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Backups locais: copia jotes.db (+ -wal/-shm) e a árvore attachments/ para
 * {@code <pasta da bd>/backups/<timestamp>/}. Também constrói/restaura o mesmo
 * conteúdo como um único ZIP em memória (usado pelo backup encriptado).
 */
public class BackupService {
    /** Número de backups automáticos a manter. */
    public static final int KEEP_BACKUPS = 10;

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** Varrimento do agendador: de hora a hora chega para intervalos medidos em horas. */
    private static final int SCHEDULER_MS = 60 * 60 * 1000;

    private final Database db;
    private final Path dataDir;
    private javax.swing.Timer scheduler;

    public BackupService(Database db) {
        this.db = db;
        this.dataDir = db.dir();
    }

    /**
     * Liga o backup automático agendado a partir das definições
     * ({@link SettingsRepository#BACKUP_INTERVAL_HOURS}; {@code 0} desliga).
     * Verifica de hora a hora se já passou o intervalo desde o último backup —
     * assim um intervalo diário sobrevive a a app ficar fechada de um dia para o outro.
     */
    public void startScheduler(SettingsRepository settings) {
        stopScheduler();
        scheduler = new javax.swing.Timer(SCHEDULER_MS, e -> runScheduled(settings));
        scheduler.setInitialDelay(30_000);
        scheduler.start();
    }

    public void stopScheduler() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
    }

    /** Faz um backup se o intervalo configurado já tiver passado. Devolve a pasta criada ou null. */
    public Path runScheduled(SettingsRepository settings) {
        try {
            int hours = settings.getInt(SettingsRepository.BACKUP_INTERVAL_HOURS, 0);
            if (hours <= 0) return null;
            long last = settings.getLong(SettingsRepository.BACKUP_LAST_AT, 0);
            long now = System.currentTimeMillis();
            if (now - last < hours * 3600_000L) return null;

            Path created = createBackup("auto");
            pruneBackups(settings.getInt(SettingsRepository.BACKUP_KEEP, KEEP_BACKUPS));
            settings.set(SettingsRepository.BACKUP_LAST_AT, now);
            return created;
        } catch (Exception ex) {
            Log.warn(BackupService.class, "Backup automático falhou", ex);
            return null;
        }
    }

    /** Backup automático no arranque: cria um backup e poda os antigos. Devolve a pasta criada. */
    public Path createStartupBackup() throws Exception {
        Path created = createBackup("auto");
        pruneBackups(KEEP_BACKUPS);
        return created;
    }

    /** Copia a bd e os anexos para {@code backups/<timestamp>[-<etiqueta>]/}. Devolve a pasta criada. */
    public Path createBackup(String label) throws Exception {
        checkpoint();
        String name = STAMP.format(LocalDateTime.now()) + (label == null || label.isBlank() ? "" : "-" + label);
        Path dest = dataDir.resolve("backups").resolve(name);
        // evita colisão se dois backups caírem no mesmo segundo
        int i = 2;
        while (Files.exists(dest)) dest = dataDir.resolve("backups").resolve(name + "-" + i++);
        Files.createDirectories(dest);
        copyDbFiles(dest);
        Path attachments = dataDir.resolve("attachments");
        if (Files.isDirectory(attachments)) copyTree(attachments, dest.resolve("attachments"));
        return dest;
    }

    /** Constrói um ZIP em memória com jotes.db (+ -wal/-shm) e attachments/. */
    public byte[] buildBackupZip() throws Exception {
        checkpoint();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            addFile(zos, db.file(), "jotes.db");
            addIfExists(zos, sibling("-wal"), "jotes.db-wal");
            addIfExists(zos, sibling("-shm"), "jotes.db-shm");
            Path attachments = dataDir.resolve("attachments");
            if (Files.isDirectory(attachments)) {
                try (Stream<Path> walk = Files.walk(attachments)) {
                    for (Path p : walk.filter(Files::isRegularFile).toList()) {
                        String rel = attachments.relativize(p).toString().replace('\\', '/');
                        addFile(zos, p, "attachments/" + rel);
                    }
                }
            }
        }
        return bos.toByteArray();
    }

    /**
     * Restaura um ZIP produzido por {@link #buildBackupZip()} para a pasta de dados,
     * substituindo jotes.db e attachments/. Os ficheiros -wal/-shm antigos são removidos.
     * A aplicação deve ser reiniciada a seguir.
     */
    public void restoreBackupZip(byte[] zipBytes) throws Exception {
        if (!db.conn().isClosed()) checkpoint();
        boolean sawDb = false;
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\', '/');
                Path target;
                if (name.equals("jotes.db")) {
                    target = db.file();
                    sawDb = true;
                } else if (name.startsWith("attachments/")) {
                    target = dataDir.resolve("attachments").resolve(name.substring("attachments/".length()));
                } else {
                    continue; // ignora -wal/-shm do zip e entradas desconhecidas
                }
                // proteção contra zip slip
                if (!target.normalize().startsWith(dataDir.normalize())) continue;
                if (target.getParent() != null) Files.createDirectories(target.getParent());
                Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (!sawDb) throw new IOException("O backup não contém a base de dados (jotes.db).");
        // o WAL antigo já não corresponde à bd restaurada
        Files.deleteIfExists(sibling("-wal"));
        Files.deleteIfExists(sibling("-shm"));
    }

    /** Apaga os backups mais antigos, mantendo apenas os {@code keep} mais recentes. */
    public void pruneBackups(int keep) throws IOException {
        Path backupsDir = dataDir.resolve("backups");
        if (!Files.isDirectory(backupsDir)) return;
        List<Path> dirs;
        try (Stream<Path> s = Files.list(backupsDir)) {
            dirs = s.filter(Files::isDirectory).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
        }
        for (int i = 0; i < dirs.size() - keep; i++) deleteTree(dirs.get(i));
    }

    /** Força o checkpoint do WAL para que jotes.db fique consistente antes de copiar. */
    private void checkpoint() throws SQLException {
        try (Statement st = db.conn().createStatement()) {
            st.execute("PRAGMA wal_checkpoint(FULL)");
        }
    }

    private Path sibling(String suffix) {
        return db.file().resolveSibling(db.file().getFileName() + suffix);
    }

    private void copyDbFiles(Path dest) throws IOException {
        Files.copy(db.file(), dest.resolve("jotes.db"), StandardCopyOption.REPLACE_EXISTING);
        for (String suffix : new String[]{"-wal", "-shm"}) {
            Path extra = sibling(suffix);
            if (Files.exists(extra)) {
                Files.copy(extra, dest.resolve("jotes.db" + suffix), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void addIfExists(ZipOutputStream zos, Path p, String name) throws IOException {
        if (Files.exists(p)) addFile(zos, p, name);
    }

    private void addFile(ZipOutputStream zos, Path p, String name) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        try (InputStream in = Files.newInputStream(p)) {
            in.transferTo(zos);
        }
        zos.closeEntry();
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path p : walk.toList()) {
                Path rel = from.relativize(p);
                Path dest = to.resolve(rel);
                if (Files.isDirectory(p)) Files.createDirectories(dest);
                else Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
