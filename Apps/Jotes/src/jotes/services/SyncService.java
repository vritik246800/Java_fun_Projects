package jotes.services;

import jotes.db.SettingsRepository;
import jotes.util.Json;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

/**
 * Sincronização por pasta: usa um ficheiro {@code jotes-sync.json} numa pasta à escolha
 * (pode ser uma pasta sincronizada pela cloud, p.ex. Dropbox/OneDrive).
 * <p>Em cada sincronização: lê o ficheiro remoto (se existir), faz merge local por uuid
 * (last-write-wins por updated_at, via importação) e escreve de volta o resultado fundido.
 * Não apaga notas — apenas adiciona/atualiza.
 */
public class SyncService {
    public static final String SYNC_FILE_NAME = "jotes-sync.json";

    private final SettingsRepository settings;
    private final ExportImportService exportImport;

    public SyncService(SettingsRepository settings, ExportImportService exportImport) {
        this.settings = settings;
        this.exportImport = exportImport;
    }

    /** Pasta de sincronização configurada, ou null se ainda não foi escolhida. */
    public String syncFolder() throws SQLException {
        return settings.get(SettingsRepository.SYNC_FOLDER, null);
    }

    public void setSyncFolder(String path) throws SQLException {
        settings.set(SettingsRepository.SYNC_FOLDER, path);
    }

    /**
     * Sincroniza com a pasta configurada. Devolve o número de notas locais
     * inseridas/atualizadas a partir do ficheiro remoto.
     */
    public int syncNow() throws Exception {
        String folder = syncFolder();
        if (folder == null || folder.isBlank()) {
            throw new IllegalStateException("Pasta de sincronização não definida.");
        }
        Path dir = Paths.get(folder);
        Files.createDirectories(dir);
        Path file = dir.resolve(SYNC_FILE_NAME);

        int updated = 0;
        if (Files.exists(file)) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Object parsed;
            try {
                parsed = Json.parse(text);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "O ficheiro de sincronização está corrompido; não foi alterado. (" + e.getMessage() + ")");
            }
            updated = exportImport.importDocument(Json.asMap(parsed));
        }

        // escreve o resultado fundido (via ficheiro temporário para não corromper o sync file)
        String out = Json.write(exportImport.buildDocument());
        Path tmp = dir.resolve(SYNC_FILE_NAME + ".tmp");
        Files.writeString(tmp, out, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return updated;
    }
}
