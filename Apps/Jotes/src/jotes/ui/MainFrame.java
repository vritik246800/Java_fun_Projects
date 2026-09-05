package jotes.ui;

import jotes.db.AttachmentRepository;
import jotes.db.CanvasRepository;
import jotes.db.Database;
import jotes.db.FolderRepository;
import jotes.db.HistoryRepository;
import jotes.db.LinkRepository;
import jotes.db.NoteRepository;
import jotes.db.SettingsRepository;
import jotes.db.TagRepository;
import jotes.model.Folder;
import jotes.model.Note;
import jotes.services.AttachmentStorage;
import jotes.services.BackupService;
import jotes.services.CryptoService;
import jotes.services.ExportImportService;
import jotes.services.HtmlExportService;
import jotes.services.MarkdownImportService;
import jotes.services.PdfExportService;
import jotes.services.ReminderService;
import jotes.services.SyncService;
import jotes.ui.canvas.CanvasWindow;
import jotes.ui.editor.Templates;
import jotes.util.Log;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Janela principal: sidebar, lista de notas, separadores, editor e painel de
 * backlinks, num par de divisores com posição memorizada. Regista todos os
 * comandos em {@link Shortcuts} — que servem os atalhos, a paleta (Ctrl+K) e a
 * tela de ajuda (Ctrl+/) — e liga os serviços de exportação, importação,
 * backup, sincronização e lembretes.
 */
public class MainFrame extends JFrame {

    private static final int DEFAULT_SIDEBAR = 220;
    private static final int DEFAULT_LIST = 320;

    private final Database db;
    private final NoteRepository noteRepo;
    private final FolderRepository folderRepo;
    private final TagRepository tagRepo;
    private final LinkRepository linkRepo;
    private final HistoryRepository historyRepo;
    private final AttachmentRepository attachmentRepo;
    private final CanvasRepository canvasRepo;
    private final SettingsRepository settings;

    private final AttachmentStorage attachmentStorage;
    private final BackupService backupService;
    private final SyncService syncService;
    private final ExportImportService exportImport;
    private final HtmlExportService htmlExport;
    private final PdfExportService pdfExport;
    private final MarkdownImportService markdownImport;
    private final ReminderService reminders;
    private final CryptoService crypto = new CryptoService();

    private final SidebarPanel sidebar;
    private final NotesListPanel notesList;
    private final EditorPanel editor;
    private final BacklinksPanel backlinks;
    private final NoteTabs tabs = new NoteTabs();
    private final StatusBar statusBar = new StatusBar();
    private final Shortcuts shortcuts;

    private final JPanel root = new JPanel(new BorderLayout());
    private final JSplitPane sidebarSplit;
    private final JSplitPane listSplit;
    private final JSplitPane backlinksSplit;
    private final JComponent toolbar;

    private JToggleButton themeToggle;
    private JToggleButton focusToggle;
    private JToggleButton sideBySideToggle;

    private CommandPalette palette;
    private boolean focusMode;
    private int savedSidebarPosition = DEFAULT_SIDEBAR;
    private int savedListPosition = DEFAULT_LIST;

    public MainFrame(Database db) {
        super("Jotes");
        this.db = db;
        this.noteRepo = new NoteRepository(db);
        this.folderRepo = new FolderRepository(db);
        this.tagRepo = new TagRepository(db);
        this.linkRepo = new LinkRepository(db);
        this.historyRepo = new HistoryRepository(db);
        this.attachmentRepo = new AttachmentRepository(db);
        this.canvasRepo = new CanvasRepository(db);
        this.settings = new SettingsRepository(db);

        this.attachmentStorage = new AttachmentStorage(db.dir());
        this.backupService = new BackupService(db);
        this.exportImport = new ExportImportService(noteRepo, folderRepo, tagRepo, linkRepo);
        this.syncService = new SyncService(settings, exportImport);
        this.htmlExport = new HtmlExportService(noteRepo, folderRepo, attachmentStorage);
        this.pdfExport = new PdfExportService(attachmentStorage);
        this.markdownImport = new MarkdownImportService(noteRepo, folderRepo, tagRepo, linkRepo);
        this.reminders = new ReminderService(db);
        this.shortcuts = new Shortcuts(settings);

        this.sidebar = new SidebarPanel(folderRepo, tagRepo);
        this.notesList = new NotesListPanel(noteRepo, folderRepo);
        this.editor = new EditorPanel(noteRepo, tagRepo, attachmentRepo, attachmentStorage,
                historyRepo, linkRepo);
        this.backlinks = new BacklinksPanel(linkRepo);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 560));

        notesList.setTagRepository(tagRepo);

        // editor + separadores + backlinks à direita
        JPanel editorArea = new JPanel(new BorderLayout());
        editorArea.setBackground(Theme.BG);
        editorArea.add(tabs, BorderLayout.NORTH);
        editorArea.add(editor, BorderLayout.CENTER);

        backlinksSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorArea, backlinks);
        Theme.styleSplitPane(backlinksSplit);
        backlinksSplit.setResizeWeight(1);
        backlinksSplit.setDividerLocation(0.76);

        listSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, notesList, backlinksSplit);
        Theme.styleSplitPane(listSplit);
        listSplit.setDividerLocation(DEFAULT_LIST);

        sidebarSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, listSplit);
        Theme.styleSplitPane(sidebarSplit);
        sidebarSplit.setDividerLocation(DEFAULT_SIDEBAR);

        toolbar = buildToolbar();

        root.setBackground(Theme.BG);
        root.add(toolbar, BorderLayout.NORTH);
        root.add(sidebarSplit, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        setContentPane(root);

        wireCallbacks();
        registerCommands();
        // o Swing consome Ctrl+Tab para navegação de foco: sem isto o atalho dos
        // separadores nunca chega ao InputMap da janela. Só Ctrl+Tab é retirado —
        // o Tab simples continua a percorrer os campos.
        freeCtrlTab();
        shortcuts.install(root);
        restoreLayout();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { Toast.reposition(MainFrame.this); }
        });

        reminders.setOnDue(this::showReminder);
        reminders.start();
        backupService.startScheduler(settings);

        notesList.showNotes(sidebar.current());
        updateStatusContext();
    }

    /** Retira Ctrl+Tab / Ctrl+Shift+Tab da navegação de foco, mantendo o Tab simples. */
    private void freeCtrlTab() {
        for (int id : new int[]{java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS}) {
            java.util.Set<java.awt.AWTKeyStroke> keys =
                    new java.util.HashSet<>(getFocusTraversalKeys(id));
            keys.removeIf(k -> (k.getModifiers() & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0);
            setFocusTraversalKeys(id, keys);
        }
    }

    // ---------------------------------------------------------------- barra superior

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(Theme.SIDEBAR_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR));

        bar.add(toolButton(Icons.SEARCH, "Procurar tudo (Ctrl+K)", this::openPalette));
        bar.add(toolButton(Icons.PLUS, "Nova nota (Ctrl+N)", notesList::requestNewNote));
        bar.add(toolButton(Icons.CHECKLIST, "Nova nota a partir de modelo", this::newFromTemplate));
        bar.add(toolButton(Icons.CLOCK, "Nota diária", this::openDailyNote));
        bar.add(separator());
        bar.add(toolButton(Icons.GRAPH, "Grafo de notas", this::openGraph));
        bar.add(toolButton(Icons.CANVAS, "Canvas", this::openCanvas));
        bar.add(toolButton(Icons.CHART, "Estatísticas", this::openStats));
        bar.add(separator());
        bar.add(toolButton(Icons.EXPORT, "Exportar…", this::exportMenu));
        bar.add(toolButton(Icons.IMPORT, "Importar…", this::importMenu));
        bar.add(toolButton(Icons.SYNC, "Sincronizar agora", this::syncNow));
        bar.add(separator());

        focusToggle = new JToggleButton(Icons.themed(Icons.FOCUS, 16));
        Theme.styleToolbarButton(focusToggle);
        focusToggle.setToolTipText("Modo foco (Ctrl+Shift+F)");
        focusToggle.setFocusable(false);
        focusToggle.addActionListener(e -> setFocusMode(focusToggle.isSelected()));
        bar.add(focusToggle);

        sideBySideToggle = new JToggleButton(Icons.themed(Icons.COLUMNS, 16));
        Theme.styleToolbarButton(sideBySideToggle);
        sideBySideToggle.setToolTipText("Editar e pré-visualizar lado a lado");
        sideBySideToggle.setFocusable(false);
        sideBySideToggle.addActionListener(e -> {
            editor.setSideBySide(sideBySideToggle.isSelected());
            save(SettingsRepository.UI_SIDE_BY_SIDE, sideBySideToggle.isSelected());
        });
        bar.add(sideBySideToggle);

        themeToggle = new JToggleButton(Icons.themed(Icons.MOON, 16));
        Theme.styleToolbarButton(themeToggle);
        themeToggle.setToolTipText("Alternar tema claro/escuro");
        themeToggle.setFocusable(false);
        themeToggle.addActionListener(e -> toggleTheme());
        bar.add(themeToggle);

        bar.add(toolButton(Icons.SETTINGS, "Definições", this::openSettings));
        bar.add(toolButton(Icons.INFO, "Atalhos de teclado (Ctrl+/)", this::openShortcuts));
        return bar;
    }

    private JButton toolButton(String icon, String tooltip, Runnable action) {
        JButton b = new JButton(Icons.themed(icon, 16));
        Theme.styleToolbarButton(b);
        b.setToolTipText(tooltip);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private Component separator() {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(10, 1));
        return gap;
    }

    // ---------------------------------------------------------------- ligações

    private void wireCallbacks() {
        sidebar.setOnSelectionChanged(sel -> {
            notesList.showNotes(sel);
            updateStatusContext();
        });
        sidebar.setOnFoldersChanged(() -> {
            notesList.refresh();
            refreshBreadcrumb();
        });

        notesList.setOnNoteSelected(this::openNote);
        notesList.setOnNoteCreated(n -> {
            openNote(n);
            editor.focusTitle();
        });
        notesList.setOnNoteRenamed(n -> {
            tabs.updateTitle(n);
            refreshBreadcrumb();
        });
        notesList.setOnNoteTrashed(n -> Toast.show(root, "Nota movida para a lixeira", "Anular", () -> {
            try {
                noteRepo.restore(n.getId());
                notesList.refresh();
                sidebar.refresh();
            } catch (Exception ex) {
                Log.warn(MainFrame.class, "Falha ao restaurar nota", ex);
            }
        }));
        notesList.setOnNoteDeleted(id -> {
            editor.clearIfShowing(id);
            tabs.close(id);
            backlinks.clear();
            updateStatusContext();
        });

        editor.setOnSaved(n -> {
            notesList.refresh();
            tabs.updateTitle(n);
            backlinks.showNote(n);
            updateStatusContext();
        });
        editor.setOnTagsChanged(sidebar::refresh);
        editor.setOnNavigateToTitle(this::openNoteByTitle);
        editor.setOnShowHistory(this::openHistory);
        editor.setOnStatsChanged(statusBar::setStats);

        backlinks.setOnOpenNote(this::openNoteById);

        tabs.setOnSelect(this::openNote);
        tabs.setOnClose(n -> {
            editor.flush();
            tabs.close(n.getId());
            if (editor.getNote() != null && editor.getNote().getId() == n.getId()) {
                editor.clearIfShowing(n.getId());
                backlinks.clear();
            }
        });
    }

    // ---------------------------------------------------------------- comandos

    /** Regista todos os comandos; a lista alimenta atalhos, paleta e tela de ajuda. */
    private void registerCommands() {
        shortcuts.register("note.new", "Nova nota", "Notas", Icons.PLUS,
                Shortcuts.menu(KeyEvent.VK_N), notesList::requestNewNote);
        shortcuts.register("note.template", "Nova nota a partir de modelo", "Notas", Icons.CHECKLIST,
                Shortcuts.menuShift(KeyEvent.VK_N), this::newFromTemplate);
        shortcuts.register("note.daily", "Abrir a nota de hoje", "Notas", Icons.CLOCK,
                Shortcuts.menu(KeyEvent.VK_D), this::openDailyNote);
        shortcuts.register("note.save", "Guardar agora", "Notas", Icons.SAVE,
                Shortcuts.menu(KeyEvent.VK_S), editor::flush);
        shortcuts.register("note.history", "Histórico de versões", "Notas", Icons.HISTORY,
                Shortcuts.menu(KeyEvent.VK_H), this::openHistory);
        shortcuts.register("note.reminder", "Lembrete para esta nota", "Notas", Icons.BELL,
                Shortcuts.menu(KeyEvent.VK_R), this::openReminders);
        shortcuts.register("note.lock", "Bloquear / desbloquear nota", "Notas", Icons.LOCK,
                Shortcuts.menuShift(KeyEvent.VK_L), this::toggleLock);

        shortcuts.register("search.palette", "Procurar tudo", "Navegação", Icons.SEARCH,
                Shortcuts.menu(KeyEvent.VK_K), this::openPalette);
        shortcuts.register("search.palette.alt", "Ir para nota", "Navegação", Icons.SEARCH,
                Shortcuts.menu(KeyEvent.VK_P), this::openPalette);
        shortcuts.register("search.list", "Pesquisar na lista", "Navegação", Icons.SEARCH,
                Shortcuts.menu(KeyEvent.VK_F), notesList::focusSearch);
        shortcuts.register("tabs.next", "Separador seguinte", "Navegação", Icons.NOTE,
                Shortcuts.menu(KeyEvent.VK_TAB), () -> tabs.cycle(1));
        shortcuts.register("tabs.previous", "Separador anterior", "Navegação", Icons.NOTE,
                Shortcuts.menuShift(KeyEvent.VK_TAB), () -> tabs.cycle(-1));
        shortcuts.register("tabs.close", "Fechar separador", "Navegação", Icons.CLOSE,
                Shortcuts.menu(KeyEvent.VK_W), this::closeCurrentTab);

        shortcuts.register("view.graph", "Grafo de notas", "Vistas", Icons.GRAPH,
                Shortcuts.menu(KeyEvent.VK_G), this::openGraph);
        shortcuts.register("view.canvas", "Canvas", "Vistas", Icons.CANVAS,
                Shortcuts.menuShift(KeyEvent.VK_C), this::openCanvas);
        shortcuts.register("view.stats", "Estatísticas", "Vistas", Icons.CHART, null, this::openStats);
        shortcuts.register("view.focus", "Modo foco", "Vistas", Icons.FOCUS,
                Shortcuts.menuShift(KeyEvent.VK_F), () -> setFocusMode(!focusMode));
        shortcuts.register("view.sideBySide", "Editar e pré-visualizar lado a lado", "Vistas", Icons.COLUMNS,
                Shortcuts.menuShift(KeyEvent.VK_E), () -> {
                    sideBySideToggle.setSelected(!sideBySideToggle.isSelected());
                    editor.setSideBySide(sideBySideToggle.isSelected());
                });
        shortcuts.register("view.theme", "Alternar tema claro/escuro", "Vistas", Icons.SUN,
                Shortcuts.menuShift(KeyEvent.VK_T), this::toggleTheme);

        shortcuts.register("file.exportPdf", "Exportar nota para PDF", "Ficheiro", Icons.EXPORT,
                null, this::exportPdf);
        shortcuts.register("file.exportHtml", "Exportar pasta para site HTML", "Ficheiro", Icons.EXPORT,
                null, this::exportHtml);
        shortcuts.register("file.exportJson", "Exportar tudo (.json)", "Ficheiro", Icons.EXPORT,
                null, this::exportJson);
        shortcuts.register("file.importMarkdown", "Importar pasta de .md", "Ficheiro", Icons.IMPORT,
                null, this::importMarkdown);
        shortcuts.register("file.importJson", "Importar cópia (.json)", "Ficheiro", Icons.IMPORT,
                null, this::importJson);
        shortcuts.register("file.sync", "Sincronizar agora", "Ficheiro", Icons.SYNC, null, this::syncNow);
        shortcuts.register("file.backup", "Fazer backup agora", "Ficheiro", Icons.SAVE, null, this::backupNow);
        shortcuts.register("file.emptyTrash", "Esvaziar lixeira", "Ficheiro", Icons.TRASH, null, this::emptyTrash);

        shortcuts.register("app.settings", "Definições", "Aplicação", Icons.SETTINGS,
                Shortcuts.menu(KeyEvent.VK_COMMA), this::openSettings);
        shortcuts.register("app.shortcuts", "Atalhos de teclado", "Aplicação", Icons.INFO,
                Shortcuts.menu(KeyEvent.VK_SLASH), this::openShortcuts);
    }

    // ---------------------------------------------------------------- notas

    private void openNote(Note note) {
        if (note == null) return;
        editor.showNote(note);
        tabs.open(note);
        tabs.setActive(note.getId());
        notesList.selectNote(note.getId());
        backlinks.showNote(note);
        refreshBreadcrumb();
    }

    private void openNoteById(long id) {
        try {
            Note n = noteRepo.findById(id);
            if (n != null) openNote(n);
        } catch (Exception ex) {
            Toast.error(root, "Não foi possível abrir a nota.");
            Log.warn(MainFrame.class, "Falha ao abrir nota " + id, ex);
        }
    }

    /** Abre a nota com este título; se não existir, oferece criá-la (link [[novo]]). */
    private void openNoteByTitle(String title) {
        try {
            Note existing = noteRepo.findByTitle(title);
            if (existing != null) {
                openNote(existing);
                return;
            }
            int ok = JOptionPane.showConfirmDialog(this,
                    "A nota «" + title + "» ainda não existe. Criar?",
                    "Nota em falta", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            Note created = noteRepo.create(sidebar.current().folderId());
            created.setTitle(title);
            noteRepo.save(created);
            notesList.refresh();
            openNote(created);
        } catch (Exception ex) {
            Toast.error(root, "Não foi possível abrir «" + title + "».");
            Log.warn(MainFrame.class, "Falha ao resolver wikilink", ex);
        }
    }

    private void closeCurrentTab() {
        Note current = editor.getNote();
        if (current == null) return;
        editor.flush();
        tabs.close(current.getId());
        editor.clearIfShowing(current.getId());
        backlinks.clear();
    }

    /** Cria uma nota já preenchida com um modelo escolhido. */
    private void newFromTemplate() {
        List<Templates.Template> templates = Templates.all();
        String[] names = templates.stream().map(Templates.Template::name).toArray(String[]::new);
        Object choice = JOptionPane.showInputDialog(this, "Modelo:", "Nova nota",
                JOptionPane.PLAIN_MESSAGE, null, names, names[1]);
        if (choice == null) return;
        applyTemplate(Templates.byName(choice.toString()), null);
    }

    /** Abre (ou cria) a nota do dia, sempre com o mesmo título {@code aaaa-mm-dd}. */
    private void openDailyNote() {
        String title = Templates.dailyTitle();
        try {
            Note existing = noteRepo.findByTitle(title);
            if (existing != null) {
                openNote(existing);
                Toast.show(root, "Nota de hoje aberta");
                return;
            }
            applyTemplate(Templates.daily(), title);
            Toast.show(root, "Nota de hoje criada");
        } catch (Exception ex) {
            Toast.error(root, "Não foi possível abrir a nota de hoje.");
            Log.warn(MainFrame.class, "Falha na nota diária", ex);
        }
    }

    /** Cria a nota com o modelo indicado; {@code forcedTitle} sobrepõe o título do modelo. */
    private void applyTemplate(Templates.Template template, String forcedTitle) {
        try {
            SidebarPanel.Selection sel = sidebar.current();
            Note note = noteRepo.create(sel.trash() || sel.archived() ? null : sel.folderId());
            note.setTitle(forcedTitle != null ? forcedTitle : Templates.render(template.title()));
            note.setContent(Templates.render(template.body()));
            noteRepo.save(note);
            notesList.refresh();
            openNote(note);
            editor.focusTitle();
        } catch (Exception ex) {
            Toast.error(root, "Não foi possível criar a nota.");
            Log.warn(MainFrame.class, "Falha ao aplicar modelo", ex);
        }
    }

    private void openHistory() {
        Note note = editor.getNote();
        if (note == null) {
            Toast.show(root, "Abre uma nota primeiro");
            return;
        }
        editor.flush();
        HistoryDialog dialog = new HistoryDialog(this, historyRepo, note);
        dialog.setOnRestore(entry -> editor.restoreVersion(entry.getTitle(), entry.getContent()));
        dialog.setVisible(true);
    }

    private void openReminders() {
        Note note = editor.getNote();
        if (note == null) {
            Toast.show(root, "Abre uma nota primeiro");
            return;
        }
        new ReminderDialog(this, reminders, note).setVisible(true);
    }

    private void showReminder(ReminderService.Reminder reminder) {
        Note note = null;
        try {
            note = noteRepo.findById(reminder.noteId());
        } catch (Exception ignored) {
            // sem a nota o aviso continua a fazer sentido
        }
        Note target = note;
        Toast.show(root, ReminderService.describe(reminder, note), "Abrir",
                () -> { if (target != null) openNote(target); });
    }

    private void toggleLock() {
        Note note = editor.getNote();
        if (note == null) {
            Toast.show(root, "Abre uma nota primeiro");
            return;
        }
        editor.flush();
        boolean ok = NoteLockDialog.isLocked(note)
                ? NoteLockDialog.unlock(this, note, noteRepo, crypto)
                : NoteLockDialog.lock(this, note, noteRepo, crypto);
        if (!ok) return;
        editor.showNote(note);
        notesList.refresh();
        Toast.show(root, NoteLockDialog.isLocked(note) ? "Nota bloqueada" : "Nota desbloqueada");
    }

    // ---------------------------------------------------------------- vistas

    private void openPalette() {
        if (palette == null) {
            palette = new CommandPalette(this, noteRepo, tagRepo, shortcuts);
            palette.setOnOpenNote(this::openNote);
            palette.setOnOpenTag(tag -> sidebar.select(SidebarPanel.Selection.ofTag(tag)));
        }
        editor.flush();
        palette.open();
    }

    private void openGraph() {
        GraphWindow window = new GraphWindow(noteRepo, linkRepo, folderRepo, tagRepo);
        window.setOnOpenNote(this::openNoteById);
        window.setVisible(true);
    }

    private void openCanvas() {
        CanvasWindow window = new CanvasWindow(canvasRepo, noteRepo);
        window.setOnOpenNote(this::openNoteById);
        window.setVisible(true);
    }

    private void openStats() {
        new StatsDialog(this, noteRepo).setVisible(true);
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, settings, backupService, syncService);
        dialog.setOnThemeChanged(this::syncThemeToggle);
        dialog.setVisible(true);
        backupService.startScheduler(settings);
    }

    private void openShortcuts() {
        new ShortcutsDialog(this, shortcuts).setVisible(true);
    }

    private void toggleTheme() {
        boolean dark = !Theme.isDark();
        Theme.setDark(dark);
        save(SettingsRepository.UI_DARK, dark);
        syncThemeToggle();
    }

    private void syncThemeToggle() {
        themeToggle.setSelected(!Theme.isDark());
        themeToggle.setIcon(Icons.themed(Theme.isDark() ? Icons.MOON : Icons.SUN, 16));
        repaint();
    }

    /** Modo foco: esconde sidebar, lista e backlinks, deixando só o editor. */
    private void setFocusMode(boolean on) {
        if (focusMode == on) return;
        focusMode = on;
        focusToggle.setSelected(on);
        if (on) {
            savedSidebarPosition = sidebarSplit.getDividerLocation();
            savedListPosition = listSplit.getDividerLocation();
            sidebar.setVisible(false);
            notesList.setVisible(false);
            backlinks.setVisible(false);
            sidebarSplit.setDividerLocation(0);
            listSplit.setDividerLocation(0);
            backlinksSplit.setDividerLocation(1.0);
        } else {
            sidebar.setVisible(true);
            notesList.setVisible(true);
            backlinks.setVisible(true);
            sidebarSplit.setDividerLocation(savedSidebarPosition);
            listSplit.setDividerLocation(savedListPosition);
            backlinksSplit.setDividerLocation(0.76);
        }
        sidebarSplit.setDividerSize(on ? 0 : 1);
        listSplit.setDividerSize(on ? 0 : 1);
        revalidate();
        repaint();
    }

    // ---------------------------------------------------------------- ficheiro

    private void exportMenu() {
        String[] options = {"Nota atual para PDF", "Pasta para site HTML", "Tudo para .json"};
        Object choice = JOptionPane.showInputDialog(this, "O que exportar?", "Exportar",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == null) return;
        if (choice.equals(options[0])) exportPdf();
        else if (choice.equals(options[1])) exportHtml();
        else exportJson();
    }

    private void importMenu() {
        String[] options = {"Pasta de ficheiros .md", "Cópia .json do Jotes"};
        Object choice = JOptionPane.showInputDialog(this, "O que importar?", "Importar",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == null) return;
        if (choice.equals(options[0])) importMarkdown();
        else importJson();
    }

    private void exportPdf() {
        Note note = editor.getNote();
        if (note == null) {
            Toast.show(root, "Abre uma nota primeiro");
            return;
        }
        editor.flush();
        Path target = chooseSaveFile("Exportar para PDF",
                HtmlExportService.slug(note.displayTitle()) + ".pdf", "PDF", "pdf");
        if (target == null) return;
        runInBackground("A exportar…", () -> {
            pdfExport.export(note, target, Theme.current());
            return "PDF guardado em " + target.getFileName();
        });
    }

    private void exportHtml() {
        SidebarPanel.Selection sel = sidebar.current();
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Pasta de destino do site");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = fc.getSelectedFile().toPath();
        Long folderId = sel.folderId();
        editor.flush();
        runInBackground("A gerar site…", () -> {
            int count = htmlExport.export(target, folderId, Theme.current());
            return count == 0 ? "Nada para exportar" : count + " notas exportadas para " + target.getFileName();
        });
    }

    private void exportJson() {
        Path target = chooseSaveFile("Exportar tudo", "jotes-export.json", "JSON", "json");
        if (target == null) return;
        editor.flush();
        runInBackground("A exportar…", () -> exportImport.exportFile(target) + " notas exportadas");
    }

    private void importMarkdown() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Pasta com ficheiros .md");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path source = fc.getSelectedFile().toPath();

        JCheckBox subfolders = new JCheckBox("Criar uma pasta por subpasta", true);
        subfolders.setOpaque(false);
        JComboBox<String> destination = new JComboBox<>();
        destination.addItem("Sem pasta");
        List<Folder> folders = new ArrayList<>();
        try {
            folders.addAll(folderRepo.list());
            for (Folder f : folders) destination.addItem(f.getName());
        } catch (Exception ignored) {
            // segue só com "Sem pasta"
        }
        JPanel options = new JPanel();
        options.setLayout(new javax.swing.BoxLayout(options, javax.swing.BoxLayout.Y_AXIS));
        options.add(subfolders);
        options.add(Box.createVerticalStrut(8));
        options.add(new javax.swing.JLabel("Pasta de destino dos ficheiros da raiz:"));
        options.add(destination);
        if (JOptionPane.showConfirmDialog(this, options, "Importar markdown",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        int index = destination.getSelectedIndex();
        Long targetFolder = index <= 0 ? null : folders.get(index - 1).getId();
        boolean useSubfolders = subfolders.isSelected();

        runInBackground("A importar…", () -> {
            MarkdownImportService.Result result =
                    markdownImport.importTree(source, useSubfolders, targetFolder);
            if (!result.errors().isEmpty()) {
                Log.of(MainFrame.class).warn("Importação com {} erros: {}",
                        result.errors().size(), result.errors());
            }
            return result.imported() + " notas importadas"
                    + (result.skipped() > 0 ? " (" + result.skipped() + " ignoradas)" : "");
        });
    }

    private void importJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar cópia");
        fc.setFileFilter(new FileNameExtensionFilter("JSON do Jotes", "json"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path source = fc.getSelectedFile().toPath();
        runInBackground("A importar…", () -> exportImport.importFile(source) + " notas importadas");
    }

    private void syncNow() {
        runInBackground("A sincronizar…", () -> {
            int updated = syncService.syncNow();
            return updated == 0 ? "Sincronizado" : updated + " nota(s) atualizada(s)";
        });
    }

    private void backupNow() {
        runInBackground("A criar backup…", () -> {
            Path created = backupService.createBackup("manual");
            backupService.pruneBackups(settings.getInt(SettingsRepository.BACKUP_KEEP,
                    BackupService.KEEP_BACKUPS));
            return "Backup criado em " + created.getFileName();
        });
    }

    private void emptyTrash() {
        int ok = JOptionPane.showConfirmDialog(this,
                "Apagar para sempre todas as notas da lixeira?",
                "Esvaziar lixeira", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            int removed = noteRepo.emptyTrash();
            notesList.refresh();
            sidebar.refresh();
            Toast.show(root, removed + " nota(s) apagada(s) definitivamente");
        } catch (Exception ex) {
            Toast.error(root, "Não foi possível esvaziar a lixeira.");
            Log.warn(MainFrame.class, "Falha ao esvaziar lixeira", ex);
        }
    }

    private Path chooseSaveFile(String title, String suggested, String description, String extension) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setSelectedFile(new java.io.File(suggested));
        fc.setFileFilter(new FileNameExtensionFilter(description, extension));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        Path target = fc.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase().endsWith("." + extension)) {
            target = target.resolveSibling(target.getFileName() + "." + extension);
        }
        return target;
    }

    /** Corre uma tarefa demorada fora da thread da UI e mostra o resultado num aviso. */
    private void runInBackground(String busyMessage, BackgroundTask task) {
        Toast.show(root, busyMessage);
        new javax.swing.SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return task.run(); }

            @Override protected void done() {
                try {
                    Toast.show(root, get());
                    notesList.refresh();
                    sidebar.refresh();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    Toast.error(root, cause.getMessage() == null ? "Falhou" : cause.getMessage());
                    Log.warn(MainFrame.class, "Tarefa em segundo plano falhou", cause);
                }
            }
        }.execute();
    }

    /** Tarefa demorada que devolve a mensagem a mostrar no fim. */
    @FunctionalInterface
    private interface BackgroundTask {
        String run() throws Exception;
    }

    // ---------------------------------------------------------------- estado

    /** Caminho da nota aberta, mostrado por cima do título no editor. */
    private void refreshBreadcrumb() {
        Note note = editor.getNote();
        if (note == null) {
            editor.setBreadcrumb(null);
            return;
        }
        String folder = "Sem pasta";
        if (note.getFolderId() != null) {
            try {
                for (Folder f : folderRepo.list()) {
                    if (f.getId() == note.getFolderId()) {
                        folder = f.getName();
                        break;
                    }
                }
            } catch (Exception ignored) {
                // fica "Sem pasta"
            }
        }
        editor.setBreadcrumb(folder + "  ›  " + note.displayTitle());
    }

    private void updateStatusContext() {
        try {
            SidebarPanel.Selection sel = sidebar.current();
            int count = noteRepo.count(NoteRepository.Query.all()
                    .withFolder(sel.folderId())
                    .withPinnedOnly(sel.pinnedOnly())
                    .withArchived(sel.archived())
                    .withTrash(sel.trash())
                    .withTag(sel.tag()));
            statusBar.setContext(count + (count == 1 ? " nota" : " notas"));
        } catch (Exception ignored) {
            statusBar.setContext(" ");
        }
    }

    /** Lê tema, animações, divisores e geometria das definições. */
    private void restoreLayout() {
        try {
            Theme.setDark(settings.getBoolean(SettingsRepository.UI_DARK, true));
            jotes.ui.anim.Animations.setEnabled(
                    !settings.getBoolean(SettingsRepository.UI_REDUCE_ANIMATIONS, false));

            savedSidebarPosition = settings.getInt(SettingsRepository.UI_SPLIT_SIDEBAR, DEFAULT_SIDEBAR);
            savedListPosition = settings.getInt(SettingsRepository.UI_SPLIT_LIST, DEFAULT_LIST);
            sidebarSplit.setDividerLocation(savedSidebarPosition);
            listSplit.setDividerLocation(savedListPosition);

            boolean sideBySide = settings.getBoolean(SettingsRepository.UI_SIDE_BY_SIDE, false);
            sideBySideToggle.setSelected(sideBySide);
            editor.setSideBySide(sideBySide);

            String geometry = settings.get(SettingsRepository.UI_WINDOW, null);
            if (geometry != null && geometry.matches("-?\\d+,-?\\d+,\\d+,\\d+,(true|false)")) {
                String[] parts = geometry.split(",");
                setBounds(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                if (Boolean.parseBoolean(parts[4])) setExtendedState(MAXIMIZED_BOTH);
            } else {
                setSize(1280, 800);
                setLocationRelativeTo(null);
            }
        } catch (Exception ex) {
            Log.warn(MainFrame.class, "Não foi possível restaurar o layout", ex);
            setSize(1280, 800);
            setLocationRelativeTo(null);
        }
        syncThemeToggle();
    }

    /** Guarda a posição dos divisores e a geometria da janela. */
    private void saveLayout() {
        try {
            if (!focusMode) {
                settings.set(SettingsRepository.UI_SPLIT_SIDEBAR, sidebarSplit.getDividerLocation());
                settings.set(SettingsRepository.UI_SPLIT_LIST, listSplit.getDividerLocation());
            } else {
                settings.set(SettingsRepository.UI_SPLIT_SIDEBAR, savedSidebarPosition);
                settings.set(SettingsRepository.UI_SPLIT_LIST, savedListPosition);
            }
            boolean maximized = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
            java.awt.Rectangle bounds = maximized ? new java.awt.Rectangle(0, 0, 1280, 800) : getBounds();
            settings.set(SettingsRepository.UI_WINDOW, bounds.x + "," + bounds.y + ","
                    + bounds.width + "," + bounds.height + "," + maximized);
        } catch (Exception ex) {
            Log.warn(MainFrame.class, "Não foi possível guardar o layout", ex);
        }
    }

    private void save(String key, boolean value) {
        try {
            settings.set(key, value);
        } catch (Exception ex) {
            Log.warn(MainFrame.class, "Não foi possível guardar " + key, ex);
        }
    }

    /** Grava o que falta e fecha a base de dados antes de sair. */
    private void shutdown() {
        editor.flush();
        saveLayout();
        reminders.stop();
        backupService.stopScheduler();
        db.close();
        dispose();
        System.exit(0);
    }

    /** Mostra a janela e dá o foco à lista de notas. */
    public void showApp() {
        setVisible(true);
        SwingUtilities.invokeLater(notesList::focusSearch);
    }
}
