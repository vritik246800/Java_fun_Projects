package jotes.services;

import jotes.db.FolderRepository;
import jotes.db.LinkRepository;
import jotes.db.NoteRepository;
import jotes.db.TagRepository;
import jotes.model.Folder;
import jotes.model.Note;
import jotes.util.Json;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Exportação/importação de todos os dados para/de um único ficheiro JSON.
 * Formato (version 1):
 * <pre>{
 *   "app": "jotes", "version": 1, "exported_at": &lt;millis&gt;,
 *   "folders": [{"name": "..."}],
 *   "notes": [{"uuid","title","content","folder","created_at","updated_at","pinned","archived","tags":[...]}],
 *   "links": [{"source": "&lt;uuid&gt;", "target": "&lt;uuid&gt;"}]
 * }</pre>
 * A importação faz upsert por uuid (last-write-wins via {@link NoteRepository#upsertFromSync}).
 */
public class ExportImportService {
    public static final String APP_MARKER = "jotes";
    public static final int FORMAT_VERSION = 1;

    private final NoteRepository noteRepo;
    private final FolderRepository folderRepo;
    private final TagRepository tagRepo;
    private final LinkRepository linkRepo;

    public ExportImportService(NoteRepository noteRepo, FolderRepository folderRepo,
                               TagRepository tagRepo, LinkRepository linkRepo) {
        this.noteRepo = noteRepo;
        this.folderRepo = folderRepo;
        this.tagRepo = tagRepo;
        this.linkRepo = linkRepo;
    }

    /** Exporta tudo para o ficheiro indicado; devolve o número de notas exportadas. */
    public int exportFile(Path file) throws Exception {
        Map<String, Object> doc = buildDocument();
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, Json.write(doc), StandardCharsets.UTF_8);
        return Json.asList(doc.get("notes")).size();
    }

    /** Importa do ficheiro indicado; devolve o número de notas inseridas/atualizadas. */
    public int importFile(Path file) throws Exception {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        Object parsed;
        try {
            parsed = Json.parse(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ficheiro JSON inválido: " + e.getMessage());
        }
        return importDocument(Json.asMap(parsed));
    }

    /** Constrói o documento JSON com o estado atual da base de dados. */
    public Map<String, Object> buildDocument() throws SQLException {
        List<Folder> folders = folderRepo.list();
        Map<Long, String> folderNameById = new HashMap<>();
        for (Folder f : folders) folderNameById.put(f.getId(), f.getName());

        List<Note> notes = noteRepo.listAll();
        Map<Long, String> uuidById = new HashMap<>();
        for (Note n : notes) uuidById.put(n.getId(), n.getUuid());

        List<Object> foldersJson = new ArrayList<>();
        for (Folder f : folders) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", f.getName());
            foldersJson.add(m);
        }

        List<Object> notesJson = new ArrayList<>();
        for (Note n : notes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid", n.getUuid());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("folder", n.getFolderId() == null ? null : folderNameById.get(n.getFolderId()));
            m.put("created_at", n.getCreatedAt() != null ? n.getCreatedAt().toEpochMilli() : 0L);
            m.put("updated_at", n.getUpdatedAt() != null ? n.getUpdatedAt().toEpochMilli() : 0L);
            m.put("pinned", n.isPinned());
            m.put("archived", n.isArchived());
            m.put("tags", new ArrayList<>(n.getTags()));
            notesJson.add(m);
        }

        List<Object> linksJson = new ArrayList<>();
        for (long[] link : linkRepo.allLinks()) {
            String source = uuidById.get(link[0]);
            String target = uuidById.get(link[1]);
            if (source == null || target == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", source);
            m.put("target", target);
            linksJson.add(m);
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("app", APP_MARKER);
        doc.put("version", (long) FORMAT_VERSION);
        doc.put("exported_at", System.currentTimeMillis());
        doc.put("folders", foldersJson);
        doc.put("notes", notesJson);
        doc.put("links", linksJson);
        return doc;
    }

    /**
     * Importa um documento (pastas por nome, notas por uuid last-write-wins, tags e links).
     * Devolve o número de notas efetivamente inseridas ou atualizadas.
     */
    public int importDocument(Map<String, Object> doc) throws SQLException {
        String app = Json.asString(doc.get("app"));
        if (!APP_MARKER.equals(app)) {
            throw new IllegalArgumentException("O ficheiro não é uma exportação do Jotes.");
        }
        long version = Json.asLong(doc.get("version"), -1);
        if (version < 1 || version > FORMAT_VERSION) {
            throw new IllegalArgumentException("Versão de exportação não suportada: " + version);
        }

        Map<String, Long> folderIdByName = new HashMap<>();
        for (Folder f : folderRepo.list()) {
            folderIdByName.put(f.getName().toLowerCase(Locale.ROOT), f.getId());
        }
        for (Object fo : Json.asList(doc.get("folders"))) {
            upsertFolder(Json.asString(Json.asMap(fo).get("name")), folderIdByName);
        }

        Map<String, Long> idByUuid = new HashMap<>();
        Set<String> appliedUuids = new HashSet<>();
        int applied = 0;
        for (Object no : Json.asList(doc.get("notes"))) {
            Map<String, Object> m = Json.asMap(no);
            String uuid = Json.asString(m.get("uuid"));
            if (uuid == null || uuid.isBlank()) continue;

            Note incoming = new Note();
            incoming.setUuid(uuid);
            incoming.setTitle(Json.asString(m.get("title")));
            incoming.setContent(Json.asString(m.get("content")));
            incoming.setFolderId(upsertFolder(Json.asString(m.get("folder")), folderIdByName));
            long now = System.currentTimeMillis();
            incoming.setCreatedAt(Instant.ofEpochMilli(Json.asLong(m.get("created_at"), now)));
            incoming.setUpdatedAt(Instant.ofEpochMilli(Json.asLong(m.get("updated_at"), now)));
            incoming.setPinned(Json.asBool(m.get("pinned"), false));
            incoming.setArchived(Json.asBool(m.get("archived"), false));

            Note result = noteRepo.upsertFromSync(incoming);
            idByUuid.put(uuid, result.getId());
            if (result == incoming) { // inserida ou atualizada (upsertFromSync devolve "incoming" nesses casos)
                applied++;
                appliedUuids.add(uuid);
                List<String> tags = new ArrayList<>();
                for (Object t : Json.asList(m.get("tags"))) {
                    String tag = Json.asString(t);
                    if (tag != null && !tag.isBlank()) tags.add(tag);
                }
                tagRepo.setTags(result.getId(), tags);
            }
        }

        // Links: só reconstruídos para notas que foram aplicadas, e só depois de todas existirem.
        Map<Long, List<Long>> targetsBySource = new LinkedHashMap<>();
        for (Object lo : Json.asList(doc.get("links"))) {
            Map<String, Object> m = Json.asMap(lo);
            String source = Json.asString(m.get("source"));
            String target = Json.asString(m.get("target"));
            if (source == null || target == null || !appliedUuids.contains(source)) continue;
            Long sid = idByUuid.get(source);
            Long tid = idByUuid.get(target);
            if (sid == null || tid == null || sid.equals(tid)) continue;
            targetsBySource.computeIfAbsent(sid, k -> new ArrayList<>()).add(tid);
        }
        for (Map.Entry<Long, List<Long>> e : targetsBySource.entrySet()) {
            linkRepo.syncTargets(e.getKey(), e.getValue());
        }
        return applied;
    }

    /** Devolve o id da pasta com o nome indicado, criando-a se necessário; null se o nome for vazio. */
    private Long upsertFolder(String name, Map<String, Long> folderIdByName) throws SQLException {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        Long id = folderIdByName.get(key);
        if (id != null) return id;
        Folder created = folderRepo.create(name.trim());
        folderIdByName.put(key, created.getId());
        return created.getId();
    }
}
