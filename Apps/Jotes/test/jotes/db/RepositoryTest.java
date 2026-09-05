package jotes.db;

import jotes.model.Folder;
import jotes.model.Note;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes dos repositórios contra uma base de dados SQLite temporária. */
class RepositoryTest {

    @TempDir
    Path tempDir;

    private Database db;
    private NoteRepository notes;
    private FolderRepository folders;
    private TagRepository tags;
    private LinkRepository links;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(tempDir.resolve("test.db"));
        notes = new NoteRepository(db);
        folders = new FolderRepository(db);
        tags = new TagRepository(db);
        links = new LinkRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private Note note(String title, String content) throws Exception {
        Note n = notes.create(null);
        n.setTitle(title);
        n.setContent(content);
        notes.save(n);
        return n;
    }

    @Test
    @DisplayName("a base de dados migra até à versão mais recente")
    void migratesToLatestSchema() throws Exception {
        try (var st = db.conn().createStatement(); var rs = st.executeQuery("PRAGMA user_version")) {
            assertEquals(5, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("criar e guardar uma nota preenche uuid e datas")
    void createAndSave() throws Exception {
        Note n = note("Primeira", "conteúdo");
        assertFalse(n.getUuid().isBlank());
        assertNotNull(n.getCreatedAt());

        Note loaded = notes.findById(n.getId());
        assertEquals("Primeira", loaded.getTitle());
        assertEquals("conteúdo", loaded.getContent());
        assertFalse(loaded.isDeleted());
    }

    @Test
    @DisplayName("apagar é exclusão suave e restaurar traz a nota de volta")
    void softDeleteAndRestore() throws Exception {
        Note n = note("Apagável", "");
        notes.delete(n.getId());

        assertTrue(notes.list(NoteRepository.Query.all()).isEmpty());
        List<Note> trash = notes.list(NoteRepository.Query.all().withTrash(true));
        assertEquals(1, trash.size());
        assertTrue(trash.get(0).isDeleted());

        notes.restore(n.getId());
        assertEquals(1, notes.list(NoteRepository.Query.all()).size());
        assertTrue(notes.list(NoteRepository.Query.all().withTrash(true)).isEmpty());
    }

    @Test
    @DisplayName("esvaziar a lixeira apaga só o que lá está")
    void emptyTrashOnlyRemovesTrashed() throws Exception {
        Note keep = note("Fica", "");
        Note drop = note("Sai", "");
        notes.delete(drop.getId());

        assertEquals(1, notes.emptyTrash());
        assertNull(notes.findById(drop.getId()));
        assertNotNull(notes.findById(keep.getId()));
    }

    @Test
    @DisplayName("a pesquisa encontra por título e por conteúdo")
    void searchMatchesTitleAndContent() throws Exception {
        note("Receita de pão", "farinha, água e sal");
        note("Lista de compras", "comprar farinha");

        assertEquals(1, notes.list(NoteRepository.Query.all().withText("receita")).size());
        assertEquals(2, notes.list(NoteRepository.Query.all().withText("farinha")).size());
        assertEquals(0, notes.list(NoteRepository.Query.all().withText("bicicleta")).size());
    }

    @Test
    @DisplayName("count e paginação concordam entre si")
    void countMatchesPagedListing() throws Exception {
        for (int i = 0; i < 25; i++) note("Nota " + i, "corpo");

        NoteRepository.Query q = NoteRepository.Query.all();
        assertEquals(25, notes.count(q));
        assertEquals(10, notes.list(q.withPage(10, 0)).size());
        assertEquals(5, notes.list(q.withPage(10, 20)).size());
    }

    @Test
    @DisplayName("notas fixadas vêm primeiro")
    void pinnedNotesComeFirst() throws Exception {
        note("Antiga", "");
        Note pinned = note("Fixada", "");
        pinned.setPinned(true);
        notes.updateFlags(pinned);
        note("Recente", "");

        assertEquals("Fixada", notes.list(NoteRepository.Query.all()).get(0).getTitle());
        assertEquals(1, notes.list(NoteRepository.Query.all().withPinnedOnly(true)).size());
    }

    @Test
    @DisplayName("tags são normalizadas e podem filtrar a listagem")
    void tagsAreNormalizedAndFilter() throws Exception {
        Note n = note("Com tags", "");
        tags.setTags(n.getId(), List.of("#Trabalho", " urgente ", "Trabalho"));

        assertEquals(List.of("trabalho", "urgente"), tags.tagsOf(n.getId()));
        assertEquals(1, notes.list(NoteRepository.Query.all().withTag("trabalho")).size());
        assertEquals(0, notes.list(NoteRepository.Query.all().withTag("lazer")).size());
    }

    @Test
    @DisplayName("mover uma nota entre pastas filtra corretamente")
    void moveBetweenFolders() throws Exception {
        Folder folder = folders.create("Trabalho");
        Note n = note("Relatório", "");
        notes.move(n.getId(), folder.getId());

        assertEquals(1, notes.list(NoteRepository.Query.all().withFolder(folder.getId())).size());
        notes.move(n.getId(), null);
        assertEquals(0, notes.list(NoteRepository.Query.all().withFolder(folder.getId())).size());
    }

    @Test
    @DisplayName("wikilinks geram backlinks resolvidos por título")
    void wikilinksProduceBacklinks() throws Exception {
        Note target = note("Destino", "");
        Note source = note("Origem", "ver [[Destino]]");
        links.syncFromTitles(source.getId(), jotes.util.LinkParser.extractTitles(source.getContent()));

        List<Note> backlinks = links.backlinksOf(target.getId());
        assertEquals(1, backlinks.size());
        assertEquals("Origem", backlinks.get(0).getTitle());
        assertEquals(1, links.allLinks().size());
    }

    @Test
    @DisplayName("duplicar copia conteúdo e tags")
    void duplicateCopiesContentAndTags() throws Exception {
        Note original = note("Modelo", "corpo");
        tags.setTags(original.getId(), List.of("plano"));

        Note copy = notes.duplicate(original);
        assertEquals("Modelo (cópia)", copy.getTitle());
        assertEquals("corpo", copy.getContent());
        assertEquals(List.of("plano"), tags.tagsOf(copy.getId()));
    }

    @Test
    @DisplayName("o histórico guarda a versão anterior antes de gravar por cima")
    void historyKeepsPreviousVersion() throws Exception {
        HistoryRepository history = new HistoryRepository(db);
        Note n = note("Versionada", "v1");
        history.maybeSnapshot(n, 0);

        n.setContent("v2");
        notes.save(n);
        history.maybeSnapshot(n, 0);

        List<jotes.model.HistoryEntry> entries = history.list(n.getId());
        assertEquals(2, entries.size());
    }

    @Test
    @DisplayName("as definições guardam e leem valores tipados")
    void settingsRoundTrip() throws Exception {
        SettingsRepository settings = new SettingsRepository(db);
        settings.set(SettingsRepository.UI_DARK, false);
        settings.set(SettingsRepository.BACKUP_INTERVAL_HOURS, 24);

        assertFalse(settings.getBoolean(SettingsRepository.UI_DARK, true));
        assertEquals(24, settings.getInt(SettingsRepository.BACKUP_INTERVAL_HOURS, 0));
        assertEquals(7, settings.getInt("chave.inexistente", 7));
    }

    @Test
    @DisplayName("a query FTS escapa aspas e junta os termos com AND")
    void ftsQueryIsSafe() {
        assertEquals("\"pao\"*", NoteRepository.ftsQuery("pao"));
        assertEquals("\"receita\"* AND \"pao\"*", NoteRepository.ftsQuery("receita pao"));
        assertEquals("", NoteRepository.ftsQuery("\"\"\""));
    }

    @Test
    @DisplayName("upsert por uuid resolve o conflito pela data mais recente")
    void upsertKeepsNewestVersion() throws Exception {
        Note local = note("Local", "antigo");

        Note incoming = new Note();
        incoming.setUuid(local.getUuid());
        incoming.setTitle("Local");
        incoming.setContent("novo");
        incoming.setCreatedAt(local.getCreatedAt());
        incoming.setUpdatedAt(local.getUpdatedAt().plusSeconds(60));

        notes.upsertFromSync(incoming);
        assertEquals("novo", notes.findById(local.getId()).getContent());
    }
}
