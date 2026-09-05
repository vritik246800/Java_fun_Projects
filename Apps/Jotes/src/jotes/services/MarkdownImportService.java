package jotes.services;

import jotes.db.FolderRepository;
import jotes.db.LinkRepository;
import jotes.db.NoteRepository;
import jotes.db.TagRepository;
import jotes.model.Folder;
import jotes.model.Note;
import jotes.util.LinkParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importa ficheiros {@code .md} em massa: cada ficheiro vira uma nota, cada
 * subpasta vira uma pasta do Jotes. O título sai do primeiro {@code # cabeçalho}
 * ou, na falta dele, do nome do ficheiro; as {@code #tags} do corpo e o
 * front matter YAML ({@code title:}, {@code tags:}) são lidos quando existem.
 */
public class MarkdownImportService {

    /** Front matter YAML delimitado por {@code ---} no início do ficheiro. */
    private static final Pattern FRONT_MATTER =
            Pattern.compile("\\A---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);
    /** Tags inline {@code #assim}, ignorando cabeçalhos markdown e código. */
    private static final Pattern INLINE_TAG =
            Pattern.compile("(?<![\\w#])#([\\p{L}][\\p{L}\\p{N}_-]{0,40})");

    /** Resultado de uma importação. */
    public record Result(int imported, int skipped, List<String> errors) {}

    private final NoteRepository noteRepo;
    private final FolderRepository folderRepo;
    private final TagRepository tagRepo;
    private final LinkRepository linkRepo;

    public MarkdownImportService(NoteRepository noteRepo, FolderRepository folderRepo,
                                 TagRepository tagRepo, LinkRepository linkRepo) {
        this.noteRepo = noteRepo;
        this.folderRepo = folderRepo;
        this.tagRepo = tagRepo;
        this.linkRepo = linkRepo;
    }

    /**
     * Importa toda a árvore a partir de {@code root}.
     *
     * @param useSubfolders cria uma pasta do Jotes por subpasta do disco
     * @param targetFolder  pasta de destino para os ficheiros da raiz ({@code null} = sem pasta)
     */
    public Result importTree(Path root, boolean useSubfolders, Long targetFolder) throws IOException {
        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".md") || name.endsWith(".markdown") || name.endsWith(".txt");
                    })
                    .sorted()
                    .forEach(files::add);
        }

        Map<String, Long> foldersByName = new HashMap<>();
        try {
            for (Folder f : folderRepo.list()) foldersByName.put(f.getName(), f.getId());
        } catch (Exception ignored) {
            // sem pastas existentes; serão criadas conforme necessário
        }

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        List<Note> created = new ArrayList<>();

        for (Path file : files) {
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                Long folderId = targetFolder;
                if (useSubfolders) {
                    Path relative = root.relativize(file).getParent();
                    if (relative != null && !relative.toString().isBlank()) {
                        String name = relative.toString().replace(java.io.File.separatorChar, '/');
                        folderId = foldersByName.computeIfAbsent(name, this::createFolderQuietly);
                    }
                }
                created.add(importOne(file, text, folderId));
                imported++;
            } catch (Exception ex) {
                skipped++;
                errors.add(file.getFileName() + ": " + ex.getMessage());
            }
        }

        // segunda passagem: agora que todos os títulos existem, resolve os [[wikilinks]]
        for (Note n : created) {
            try {
                linkRepo.syncFromTitles(n.getId(), LinkParser.extractTitles(n.getContent()));
            } catch (Exception ignored) {
                // ligação por resolver não invalida a importação
            }
        }
        return new Result(imported, skipped, errors);
    }

    private Long createFolderQuietly(String name) {
        try {
            return folderRepo.create(name).getId();
        } catch (Exception e) {
            return null;
        }
    }

    /** Cria uma nota a partir do conteúdo de um ficheiro. */
    private Note importOne(Path file, String text, Long folderId) throws Exception {
        String body = text;
        String title = null;
        Set<String> tags = new LinkedHashSet<>();

        Matcher fm = FRONT_MATTER.matcher(body);
        if (fm.find()) {
            for (String line : fm.group(1).split("\n")) {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim().replaceAll("^[\"']|[\"']$", "");
                if (key.equals("title") && !value.isBlank()) title = value;
                else if (key.equals("tags")) {
                    for (String t : value.replaceAll("[\\[\\]]", "").split("[,\\s]+")) {
                        String norm = TagRepository.normalize(t);
                        if (!norm.isEmpty()) tags.add(norm);
                    }
                }
            }
            body = body.substring(fm.end());
        }

        if (title == null) {
            for (String line : body.split("\n", 12)) {
                if (line.startsWith("# ")) {
                    title = line.substring(2).trim();
                    break;
                }
            }
        }
        if (title == null || title.isBlank()) {
            String name = file.getFileName().toString();
            int dot = name.lastIndexOf('.');
            title = dot > 0 ? name.substring(0, dot) : name;
        }

        Matcher inline = INLINE_TAG.matcher(body);
        while (inline.find()) {
            String norm = TagRepository.normalize(inline.group(1));
            if (!norm.isEmpty()) tags.add(norm);
        }

        Note note = noteRepo.create(folderId);
        note.setTitle(title.trim());
        note.setContent(body.strip());
        noteRepo.save(note);
        if (!tags.isEmpty()) {
            tagRepo.setTags(note.getId(), tags);
            note.setTags(new ArrayList<>(tags));
        }
        return note;
    }
}
