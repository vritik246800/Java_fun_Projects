package jotes.ui;

import jotes.db.AttachmentRepository;
import jotes.db.HistoryRepository;
import jotes.db.LinkRepository;
import jotes.db.NoteRepository;
import jotes.db.TagRepository;
import jotes.model.Attachment;
import jotes.model.Note;
import jotes.services.AttachmentStorage;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Easing;
import jotes.ui.anim.FadeTransition;
import jotes.ui.anim.SmoothScroll;
import jotes.ui.editor.AttachmentsPanel;
import jotes.ui.editor.MarkdownActions;
import jotes.ui.editor.NoteCompletion;
import jotes.ui.editor.PreviewPane;
import jotes.ui.editor.TableEditorDialog;
import jotes.util.LinkParser;
import jotes.util.Log;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.RenderedImage;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Editor da nota (V2): título, barra de ferramentas markdown, etiquetas, anexos,
 * conteúdo em RSyntaxTextArea (syntax highlight markdown + desfazer/refazer) e
 * pré-visualização HTML em alternância. Auto-save com debounce de 600 ms.
 */
public class EditorPanel extends JPanel {

    private static final DateTimeFormatter META_FMT =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy, HH:mm").withZone(ZoneId.systemDefault());

    private final NoteRepository noteRepo;
    private final TagRepository tagRepo;
    private final AttachmentRepository attachmentRepo;
    private final AttachmentStorage attachmentStorage;
    private final HistoryRepository historyRepo;
    private final LinkRepository linkRepo;

    private final CardLayout cards;
    private final JTextField title = new Theme.PlaceholderField("Título");
    private final JLabel meta = new JLabel(" ");
    private final RSyntaxTextArea content = new RSyntaxTextArea();
    private final JTextField tagsField = new Theme.PlaceholderField("etiquetas separadas por vírgulas");
    private final AttachmentsPanel attachmentsPanel;
    private final PreviewPane previewPane;
    private final JPanel contentStack;
    private final CardLayout contentCards;
    private final RTextScrollPane editScroll;
    private final JSplitPane splitView;
    private final Timer saveTimer;

    // fades: entrada do cartão do editor, alternância edição/preview e indicador de gravação
    private final FadeTransition.FadePanel editorFade;
    private final FadeTransition.FadePanel editFade;
    private final FadeTransition.FadePanel previewFade;
    private final FadeTransition.FadePanel saveFade;
    private final JLabel saveIndicator = new JLabel(" ");
    private Animator saveAnim;

    // componentes com cores do tema, re-aplicadas em updateUI() quando a paleta muda
    private JPanel editorCard;
    private JPanel titleBlock;
    private JPanel north;
    private Theme.RoundedPanel toolbarBar;

    private JToggleButton previewToggle;
    private JButton undoButton;
    private JButton redoButton;

    private Note note;
    private boolean loading;
    private boolean sideBySide;
    /** Evita que o sincronismo de scroll entre editor e preview se realimente. */
    private boolean syncingScroll;
    private List<String> lastSavedTags = List.of();

    private final JLabel breadcrumb = new JLabel(" ");

    private Consumer<Note> onSaved = n -> {};
    private Consumer<StatusBar.Stats> onStatsChanged = st -> {};
    private Consumer<String> onNavigateToTitle = t -> {};
    private Runnable onTagsChanged = () -> {};
    private Runnable onShowHistory = () -> {};

    public EditorPanel(NoteRepository noteRepo, TagRepository tagRepo,
                       AttachmentRepository attachmentRepo, AttachmentStorage attachmentStorage,
                       HistoryRepository historyRepo, LinkRepository linkRepo) {
        super(new CardLayout());
        this.noteRepo = noteRepo;
        this.tagRepo = tagRepo;
        this.attachmentRepo = attachmentRepo;
        this.attachmentStorage = attachmentStorage;
        this.historyRepo = historyRepo;
        this.linkRepo = linkRepo;
        this.cards = (CardLayout) getLayout();

        setBackground(Theme.BG);

        // ---- cartão vazio ----
        add(new EmptyState(), "empty");

        // ---- cartão do editor ----
        editorCard = new JPanel(new BorderLayout(0, 6));
        editorCard.setBackground(Theme.BG);
        editorCard.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        // título grande em negrito, sem borda; o placeholder é pintado pelo PlaceholderField
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setBackground(Theme.BG); // o "pill" confunde-se com o fundo do editor
        title.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        meta.setFont(Theme.font(Font.PLAIN, 11));
        meta.setForeground(Theme.TEXT_DIM);
        meta.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        // caminho da nota (pasta > subpasta > nota) por cima do título
        breadcrumb.setFont(Theme.font(Font.PLAIN, 11));
        breadcrumb.setForeground(Theme.TEXT_DIM);
        breadcrumb.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 0));

        titleBlock = new JPanel(new BorderLayout());
        titleBlock.setBackground(Theme.BG);
        titleBlock.add(breadcrumb, BorderLayout.NORTH);
        titleBlock.add(title, BorderLayout.CENTER);
        // indicador de gravação à direita dos metadados: surge com "A guardar…"
        // durante o save e mostra "Guardado" que se esbate de seguida
        saveIndicator.setFont(Theme.font(Font.PLAIN, 11));
        saveIndicator.setForeground(Theme.TEXT_DIM);
        saveFade = FadeTransition.wrap(saveIndicator);
        saveFade.setAlpha(0f); // começa escondido
        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false); // herda o fundo do titleBlock
        metaRow.add(meta, BorderLayout.CENTER);
        metaRow.add(saveFade, BorderLayout.EAST);
        titleBlock.add(metaRow, BorderLayout.SOUTH);

        attachmentsPanel = new AttachmentsPanel(new AttachmentsPanel.Listener() {
            @Override public void onOpen(Attachment a) { openAttachment(a); }
            @Override public void onDelete(Attachment a) { deleteAttachment(a); }
        });

        north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(Theme.BG);
        titleBlock.setAlignmentX(LEFT_ALIGNMENT);
        JComponent toolbar = buildToolbar();
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        tagsField.setAlignmentX(LEFT_ALIGNMENT);
        attachmentsPanel.setAlignmentX(LEFT_ALIGNMENT);
        north.add(titleBlock);
        north.add(Box.createVerticalStrut(8));
        north.add(toolbar);
        north.add(Box.createVerticalStrut(6));
        north.add(tagsField);
        north.add(Box.createVerticalStrut(4));
        north.add(attachmentsPanel);
        editorCard.add(north, BorderLayout.NORTH);

        // ---- conteúdo: edição ↔ pré-visualização ----
        content.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        content.setMarkOccurrences(false);
        applyEditorTheme();
        content.setFont(Theme.font(Font.PLAIN, 15));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        editScroll = new RTextScrollPane(content);
        // visual limpo estilo Apple Notes: sem números de linha nem gutter
        editScroll.setLineNumbersEnabled(false);
        editScroll.setFoldIndicatorEnabled(false);
        Theme.styleScrollPane(editScroll);
        SmoothScroll.install(editScroll);

        previewPane = new PreviewPane(attachmentStorage);
        previewPane.setOnNavigateToTitle(t -> onNavigateToTitle.accept(t));
        previewPane.setOnToggleTask(this::toggleTask);
        previewPane.setOnEditTable(this::editTable);

        contentCards = new CardLayout();
        contentStack = new JPanel(contentCards);
        contentStack.setBackground(Theme.BG);
        editFade = FadeTransition.wrap(editScroll);
        previewFade = FadeTransition.wrap(previewPane);

        // cartão lado a lado: o editor e a pré-visualização partilham a mesma linha
        splitView = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        Theme.styleSplitPane(splitView);
        splitView.setResizeWeight(0.5);

        contentStack.add(editFade, "edit");
        contentStack.add(previewFade, "preview");
        contentStack.add(splitView, "split");
        editorCard.add(contentStack, BorderLayout.CENTER);

        installScrollSync();
        installDropTarget();
        NoteCompletion.install(content, noteRepo, tagRepo);

        editorFade = FadeTransition.wrap(editorCard);
        add(editorFade, "editor");

        // ---- auto-save com debounce ----
        saveTimer = new Timer(600, e -> saveNow());
        saveTimer.setRepeats(false);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { scheduleSave(); }
            public void removeUpdate(DocumentEvent e) { scheduleSave(); }
            public void changedUpdate(DocumentEvent e) { scheduleSave(); }
        };
        title.getDocument().addDocumentListener(dl);
        content.getDocument().addDocumentListener(dl);
        tagsField.getDocument().addDocumentListener(dl);

        content.getDocument().addUndoableEditListener(e -> updateUndoButtons());
        updateUndoButtons();

        setupShortcuts();

        cards.show(this, "empty");
    }

    // ---------------------------------------------------------------- API

    public void setOnSaved(Consumer<Note> listener) {
        this.onSaved = listener == null ? n -> {} : listener;
    }

    /** Chamado quando se clica num link [[título]] na pré-visualização. */
    public void setOnNavigateToTitle(Consumer<String> listener) {
        this.onNavigateToTitle = listener == null ? t -> {} : listener;
    }

    /** Chamado quando as etiquetas foram alteradas e guardadas. */
    public void setOnTagsChanged(Runnable listener) {
        this.onTagsChanged = listener == null ? () -> {} : listener;
    }

    /** Chamado quando se prime o botão "Histórico" da barra de ferramentas. */
    public void setOnShowHistory(Runnable listener) {
        this.onShowHistory = listener == null ? () -> {} : listener;
    }

    /** Chamado sempre que as contagens da nota mudam (alimenta a {@link StatusBar}). */
    public void setOnStatsChanged(Consumer<StatusBar.Stats> listener) {
        this.onStatsChanged = listener == null ? st -> {} : listener;
    }

    /** Caminho mostrado por cima do título, ex. {@code "Trabalho › Reuniões"}. */
    public void setBreadcrumb(String path) {
        breadcrumb.setText(path == null || path.isBlank() ? " " : path);
    }

    /** A nota aberta, ou {@code null} se o editor estiver vazio. */
    public Note getNote() {
        return note;
    }

    public void showNote(Note n) {
        flush();
        this.note = n;
        loading = true;
        title.setText(n.getTitle());
        content.setText(n.getContent());
        content.setCaretPosition(0);
        content.discardAllEdits();
        try {
            lastSavedTags = tagRepo.tagsOf(n.getId());
        } catch (Exception ex) {
            lastSavedTags = List.of();
        }
        tagsField.setText(String.join(", ", lastSavedTags));
        refreshAttachments();
        if (previewToggle.isSelected()) {
            previewToggle.setSelected(false);
            previewToggle.setIcon(Icons.themed(Icons.EYE, 15));
        }
        previewPane.invalidateCache();
        if (sideBySide) {
            contentCards.show(contentStack, "split");
            refreshPreview();
        } else {
            contentCards.show(contentStack, "edit");
        }
        applyLockState();
        updateMeta();
        updateUndoButtons();
        loading = false;
        cards.show(this, "editor");
        FadeTransition.fadeIn(editorFade, Duration.ofMillis(180));
    }

    public void clearIfShowing(long noteId) {
        if (note != null && note.getId() == noteId) {
            note = null;
            lastSavedTags = List.of();
            saveTimer.stop();
            attachmentsPanel.setAttachments(List.of());
            updateUndoButtons();
            onStatsChanged.accept(null);
            cards.show(this, "empty");
        }
    }

    /** Foca o campo do título (usado ao criar uma nota nova, para escrever de imediato). */
    public void focusTitle() {
        title.requestFocusInWindow();
    }

    /** Guarda imediatamente se houver alterações pendentes. */
    public void flush() {
        if (saveTimer.isRunning()) {
            saveTimer.stop();
            saveNow();
        }
    }

    /** Substitui título e conteúdo da nota atual por uma versão do histórico e guarda. */
    public void restoreVersion(String newTitle, String newContent) {
        if (note == null) return;
        title.setText(newTitle);
        content.setText(newContent);
        content.setCaretPosition(0);
        content.discardAllEdits();
        updateUndoButtons();
        saveNow();
    }

    // ---------------------------------------------------------------- barra de ferramentas

    /**
     * Aplica o tema do RSyntaxTextArea correspondente à paleta ativa (dark.xml ou
     * default.xml, distribuídos no jar; syntax highlight markdown); as cores base
     * (fundo, texto, caret, seleção) são depois sobrepostas com as do Theme.
     */
    private void applyEditorTheme() {
        String name = Theme.isDark() ? "dark" : "default";
        try (InputStream in = getClass().getResourceAsStream(
                "/org/fife/ui/rsyntaxtextarea/themes/" + name + ".xml")) {
            if (in != null) {
                org.fife.ui.rsyntaxtextarea.Theme.load(in).apply(content);
            }
        } catch (IOException ignored) {
            // sem tema: ficam as cores base do Theme definidas abaixo
        }
        content.setBackground(Theme.BG);
        content.setForeground(Theme.TEXT);
        content.setCaretColor(Theme.TEXT);
        content.setSelectionColor(Theme.TEXT_SELECTION);
        content.setSelectedTextColor(Theme.TEXT);
        content.setCurrentLineHighlightColor(Theme.CARD_BG);
    }

    /** Re-aplica as cores do tema quando a paleta muda (chamado por updateComponentTreeUI). */
    @Override
    public void updateUI() {
        super.updateUI();
        if (contentStack == null) return; // chamado pelo construtor do JPanel
        setBackground(Theme.BG);
        editorCard.setBackground(Theme.BG);
        titleBlock.setBackground(Theme.BG);
        north.setBackground(Theme.BG);
        contentStack.setBackground(Theme.BG);
        title.setBackground(Theme.BG);
        title.setForeground(Theme.TEXT);
        title.setCaretColor(Theme.TEXT);
        title.setSelectionColor(Theme.TEXT_SELECTION);
        title.setSelectedTextColor(Theme.TEXT);
        meta.setForeground(Theme.TEXT_DIM);
        breadcrumb.setForeground(Theme.TEXT_DIM);
        saveIndicator.setForeground(Theme.TEXT_DIM);
        tagsField.setBackground(Theme.FIELD_BG);
        tagsField.setForeground(Theme.TEXT);
        tagsField.setCaretColor(Theme.TEXT);
        tagsField.setSelectionColor(Theme.TEXT_SELECTION);
        tagsField.setSelectedTextColor(Theme.TEXT);
        toolbarBar.setFill(Theme.CARD_BG);
        toolbarBar.setBorder(Theme.roundedBorder(Theme.SEPARATOR, 1, 10));
        for (Component c : toolbarBar.getComponents()) {
            if (c instanceof AbstractButton b) b.setForeground(Theme.TEXT_DIM);
            else if (c instanceof JSeparator sepLine) sepLine.setForeground(Theme.SEPARATOR);
        }
        applyEditorTheme();
    }

    private JComponent buildToolbar() {
        toolbarBar = new Theme.RoundedPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        Theme.RoundedPanel bar = toolbarBar;
        bar.setFill(Theme.CARD_BG);
        bar.setBorder(Theme.roundedBorder(Theme.SEPARATOR, 1, 10));

        bar.add(toolButton(Icons.BOLD, "Negrito (Ctrl+B)",
                () -> MarkdownActions.wrapSelection(content, "**", "**")));
        bar.add(toolButton(Icons.ITALIC, "Itálico (Ctrl+I)",
                () -> MarkdownActions.wrapSelection(content, "*", "*")));
        bar.add(toolButton(Icons.STRIKE, "Riscado",
                () -> MarkdownActions.wrapSelection(content, "~~", "~~")));
        bar.add(sep());
        bar.add(toolButton(Icons.HEADING, "Título", () -> MarkdownActions.prefixLines(content, "# ")));
        bar.add(toolButton(Icons.LIST, "Lista com marcadores", () -> MarkdownActions.prefixLines(content, "- ")));
        bar.add(toolButton(Icons.CHECKLIST, "Lista de tarefas", () -> MarkdownActions.prefixLines(content, "- [ ] ")));
        bar.add(toolButton(Icons.QUOTE, "Citação", () -> MarkdownActions.prefixLines(content, "> ")));
        bar.add(toolButton(Icons.CODE, "Bloco de código", () -> MarkdownActions.wrapCode(content)));
        bar.add(toolButton(Icons.TABLE, "Inserir tabela", () -> MarkdownActions.insertTable(content)));
        bar.add(sep());
        bar.add(toolButton(Icons.LINK, "Link interno [[nota]]", () -> MarkdownActions.wrapInternalLink(content)));
        bar.add(toolButton(Icons.IMAGE, "Inserir imagem", this::addImage));
        bar.add(toolButton(Icons.PAPERCLIP, "Anexar ficheiro", this::addAttachment));
        bar.add(sep());

        undoButton = toolButton(Icons.UNDO, "Desfazer (Ctrl+Z)", () -> {
            if (content.canUndo()) content.undoLastAction();
            updateUndoButtons();
        });
        redoButton = toolButton(Icons.REDO, "Refazer (Ctrl+Y)", () -> {
            if (content.canRedo()) content.redoLastAction();
            updateUndoButtons();
        });
        bar.add(undoButton);
        bar.add(redoButton);
        bar.add(sep());

        previewToggle = new JToggleButton(Icons.themed(Icons.EYE, 15));
        Theme.styleToolbarButton(previewToggle);
        previewToggle.setMargin(new Insets(2, 8, 2, 8));
        previewToggle.setFocusable(false);
        previewToggle.setToolTipText("Alternar pré-visualização");
        previewToggle.addActionListener(e -> togglePreview());
        bar.add(previewToggle);

        bar.add(toolButton(Icons.HISTORY, "Histórico de versões", () -> onShowHistory.run()));
        return bar;
    }

    /** Botão de ícone da barra; a ação é ignorada quando não há nota carregada. */
    private JButton toolButton(String icon, String tooltip, Runnable action) {
        JButton b = new JButton(Icons.themed(icon, 15));
        Theme.styleToolbarButton(b);
        b.setToolTipText(tooltip);
        b.setFocusable(false);
        b.addActionListener(e -> { if (note != null) action.run(); });
        return b;
    }

    private static JSeparator sep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setForeground(Theme.SEPARATOR);
        s.setPreferredSize(new Dimension(6, 18));
        return s;
    }

    private void togglePreview() {
        if (sideBySide) return; // no modo lado a lado os dois painéis estão sempre visíveis
        boolean preview = previewToggle.isSelected();
        if (preview) previewPane.setMarkdown(content.getText());
        previewToggle.setIcon(Icons.themed(preview ? Icons.EDIT : Icons.EYE, 15));
        previewToggle.setToolTipText(preview ? "Voltar a editar" : "Alternar pré-visualização");
        // crossfade: esbate o painel atual, troca o cartão e faz surgir o novo
        FadeTransition.FadePanel outgoing = preview ? editFade : previewFade;
        FadeTransition.FadePanel incoming = preview ? previewFade : editFade;
        FadeTransition.fadeOut(outgoing, Duration.ofMillis(120), () -> {
            contentCards.show(contentStack, preview ? "preview" : "edit");
            FadeTransition.fadeIn(incoming, Duration.ofMillis(180));
        });
        if (!preview) content.requestFocusInWindow();
    }

    private void updateUndoButtons() {
        undoButton.setEnabled(note != null && content.canUndo());
        redoButton.setEnabled(note != null && content.canRedo());
    }

    // ---------------------------------------------------------------- anexos

    private void addImage() {
        if (note == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Inserir imagem");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Imagens (png, jpg, gif, bmp, webp)", "png", "jpg", "jpeg", "gif", "bmp", "webp"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            attachFile(fc.getSelectedFile().toPath());
        }
    }

    private void addAttachment() {
        if (note == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Anexar ficheiro");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            attachFile(fc.getSelectedFile().toPath());
        }
    }

    /** Copia o ficheiro para a loja, regista na BD e insere o link markdown no caret. */
    private void attachFile(Path source) {
        flush(); // garante a nota persistida antes de associar o anexo
        if (note == null) return;
        try {
            String rel = attachmentStorage.copyIn(note.getId(), source);
            Attachment a = attachmentRepo.add(note.getId(), source.getFileName().toString(),
                    rel, Files.size(attachmentStorage.resolve(rel)));
            String relMd = rel.replace(" ", "%20");
            String md = a.isImage()
                    ? "![" + a.getFilename() + "](attachment:" + relMd + ")"
                    : "[" + a.getFilename() + "](attachment:" + relMd + ")";
            content.replaceSelection(md);
            content.requestFocusInWindow();
            refreshAttachments();
        } catch (Exception ex) {
            Log.warn(EditorPanel.class, "Falha ao anexar ficheiro", ex);
            Toast.error(this, "Não foi possível anexar o ficheiro.");
        }
    }

    private void openAttachment(Attachment a) {
        if (!Desktop.isDesktopSupported()) return;
        try {
            Desktop.getDesktop().open(attachmentStorage.resolve(a.getRelpath()).toFile());
        } catch (Exception ex) {
            Log.warn(EditorPanel.class, "Falha ao abrir anexo", ex);
            Toast.error(this, "Não foi possível abrir o anexo.");
        }
    }

    private void deleteAttachment(Attachment a) {
        int r = JOptionPane.showConfirmDialog(this,
                "Remover o anexo \"" + a.getFilename() + "\"?",
                "Remover anexo", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            attachmentRepo.delete(a.getId());
            attachmentStorage.delete(a.getRelpath());
            refreshAttachments();
        } catch (Exception ex) {
            Log.warn(EditorPanel.class, "Falha ao remover anexo", ex);
            Toast.error(this, "Não foi possível remover o anexo.");
        }
    }

    private void refreshAttachments() {
        if (note == null) {
            attachmentsPanel.setAttachments(List.of());
            return;
        }
        try {
            attachmentsPanel.setAttachments(attachmentRepo.listForNote(note.getId()));
        } catch (Exception ex) {
            attachmentsPanel.setAttachments(List.of());
        }
    }

    // ---------------------------------------------------------------- atalhos de teclado

    /** Atalhos no conteúdo: Ctrl+S guarda, Ctrl+B/I/U formatam a seleção. */
    private void setupShortcuts() {
        bindShortcut(KeyEvent.VK_S, "saveNow", this::saveNow);
        bindShortcut(KeyEvent.VK_B, "bold", () -> MarkdownActions.wrapSelection(content, "**", "**"));
        bindShortcut(KeyEvent.VK_I, "italic", () -> MarkdownActions.wrapSelection(content, "*", "*"));
        bindShortcut(KeyEvent.VK_U, "underline", () -> MarkdownActions.wrapSelection(content, "<u>", "</u>"));
        bindShortcut(KeyEvent.VK_V, "colar", this::pasteFromClipboard);
    }

    /**
     * Nota bloqueada: o conteúdo é o blob cifrado, por isso o editor fica só de
     * leitura e mostra um aviso em vez do texto ilegível.
     */
    private void applyLockState() {
        boolean locked = note != null && NoteLockDialog.isLocked(note);
        content.setEditable(!locked);
        if (locked) {
            content.setText("🔒 Nota bloqueada.\n\nUsa «Bloquear / desbloquear nota» e a palavra-passe para a ler.");
            content.discardAllEdits();
        }
        title.setEditable(!locked);
        tagsField.setEditable(!locked);
        previewToggle.setEnabled(!locked && !sideBySide);
    }

    private void bindShortcut(int keyCode, String name, Runnable action) {
        content.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK), name);
        content.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (note != null) action.run();
            }
        });
    }

    // ---------------------------------------------------------------- gravação

    private void scheduleSave() {
        if (loading || note == null) return;
        note.setTitle(title.getText());
        note.setContent(content.getText());
        updateMeta();
        if (sideBySide) refreshPreview();
        saveTimer.restart();
    }

    private void saveNow() {
        if (note == null || loading) return;
        note.setTitle(title.getText());
        note.setContent(content.getText());
        List<String> tags = parseTags();
        try {
            showSaving();
            noteRepo.save(note);
            boolean tagsChanged = !tags.equals(lastSavedTags);
            tagRepo.setTags(note.getId(), tags);
            note.setTags(tags);
            linkRepo.syncFromTitles(note.getId(), LinkParser.extractTitles(note.getContent()));
            historyRepo.maybeSnapshot(note);
            lastSavedTags = tags;
            updateMeta();
            showSaved();
            onSaved.accept(note);
            if (tagsChanged) onTagsChanged.run();
        } catch (Exception ex) {
            hideSaveIndicator();
            Log.warn(EditorPanel.class, "Falha ao guardar a nota", ex);
            Toast.error(this, "Não foi possível guardar a nota.");
        }
    }

    /** Mostra "A guardar…" a surgir suavemente junto aos metadados. */
    private void showSaving() {
        if (saveAnim != null) saveAnim.cancel();
        saveIndicator.setText("A guardar…");
        saveFade.setAlpha(0f);
        saveAnim = Animator.animate(Duration.ofMillis(150), Easing.EASE_OUT,
                v -> saveFade.setAlpha((float) v), null);
    }

    /** Mostra "Guardado" opaco, aguarda um instante e esbate o indicador. */
    private void showSaved() {
        if (saveAnim != null) saveAnim.cancel();
        saveIndicator.setText("Guardado");
        saveFade.setAlpha(1f);
        saveAnim = Animator.animate(Duration.ofMillis(900), Easing.LINEAR, v -> {},
                () -> saveAnim = Animator.animate(Duration.ofMillis(400), Easing.EASE_IN_OUT,
                        v -> saveFade.setAlpha(1f - (float) v), null));
    }

    /** Esconde o indicador de imediato (usado quando a gravação falha). */
    private void hideSaveIndicator() {
        if (saveAnim != null) saveAnim.cancel();
        saveFade.setAlpha(0f);
    }

    /** Etiquetas do campo de texto: separadas por vírgulas, normalizadas, sem duplicados. */
    private List<String> parseTags() {
        List<String> out = new ArrayList<>();
        for (String part : tagsField.getText().split(",")) {
            String t = TagRepository.normalize(part);
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    // ---------------------------------------------------------------- lado a lado

    /**
     * Liga (ou desliga) o modo lado a lado. Nesse modo o botão "Pré-visualizar"
     * deixa de alternar cartões: os dois painéis estão sempre visíveis.
     */
    public void setSideBySide(boolean on) {
        if (sideBySide == on) return;
        sideBySide = on;
        previewToggle.setEnabled(!on);
        if (on) {
            // tira os painéis dos cartões e põe-nos nos dois lados do divisor
            contentStack.remove(editFade);
            contentStack.remove(previewFade);
            splitView.setLeftComponent(editFade);
            splitView.setRightComponent(previewFade);
            editFade.setAlpha(1f);
            previewFade.setAlpha(1f);
            contentCards.show(contentStack, "split");
            splitView.setDividerLocation(0.5);
            refreshPreview();
        } else {
            splitView.setLeftComponent(null);
            splitView.setRightComponent(null);
            contentStack.add(editFade, "edit");
            contentStack.add(previewFade, "preview");
            previewToggle.setSelected(false);
            previewToggle.setIcon(Icons.themed(Icons.EYE, 15));
            editFade.setAlpha(1f);
            contentCards.show(contentStack, "edit");
        }
        contentStack.revalidate();
        contentStack.repaint();
    }

    public boolean isSideBySide() {
        return sideBySide;
    }

    /** Re-renderiza a pré-visualização a partir do texto atual (usa a cache do PreviewPane). */
    private void refreshPreview() {
        if (sideBySide || previewToggle.isSelected()) previewPane.setMarkdown(content.getText());
    }

    /**
     * Mantém os dois painéis à mesma altura relativa. A sincronização é por fração
     * do scroll — é o que dá para fazer sem mapear linha a linha e chega para ler
     * a nota nos dois lados.
     */
    private void installScrollSync() {
        JScrollBar editorBar = editScroll.getVerticalScrollBar();
        editorBar.addAdjustmentListener(e -> {
            if (!sideBySide || syncingScroll) return;
            syncingScroll = true;
            try {
                int span = editorBar.getMaximum() - editorBar.getVisibleAmount() - editorBar.getMinimum();
                double fraction = span <= 0 ? 0 : (editorBar.getValue() - editorBar.getMinimum()) / (double) span;
                previewPane.setScrollFraction(fraction);
            } finally {
                syncingScroll = false;
            }
        });
        previewPane.scrollPane().getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!sideBySide || syncingScroll) return;
            syncingScroll = true;
            try {
                int span = editorBar.getMaximum() - editorBar.getVisibleAmount() - editorBar.getMinimum();
                if (span > 0) {
                    editorBar.setValue(editorBar.getMinimum()
                            + (int) Math.round(previewPane.scrollFraction() * span));
                }
            } finally {
                syncingScroll = false;
            }
        });
    }

    // ---------------------------------------------------------------- tarefas e tabelas

    /** Marca/desmarca a n-ésima caixa {@code - [ ]} do markdown a partir da pré-visualização. */
    private void toggleTask(int index) {
        if (note == null || index < 0) return;
        String updated = PreviewPane.toggleTask(content.getText(), index);
        if (updated.equals(content.getText())) return;
        int caret = content.getCaretPosition();
        content.setText(updated);
        content.setCaretPosition(Math.min(caret, updated.length()));
        previewPane.invalidateCache();
        refreshPreview();
        saveNow();
    }

    /** Abre a n-ésima tabela do markdown numa grelha editável e reescreve-a ao guardar. */
    private void editTable(int index) {
        if (note == null || index < 0) return;
        String markdown = content.getText();
        TableEditorDialog.Located table = TableEditorDialog.find(markdown, index);
        if (table == null) return;
        TableEditorDialog dialog = new TableEditorDialog(
                SwingUtilities.getWindowAncestor(this), table);
        dialog.setVisible(true);
        if (dialog.result() == null) return;
        content.setText(TableEditorDialog.replace(markdown, table, dialog.result()));
        previewPane.invalidateCache();
        refreshPreview();
        saveNow();
    }

    // ---------------------------------------------------------------- colar e largar

    /**
     * Aceita ficheiros largados no editor e imagens/ficheiros colados da área de
     * transferência, anexando-os à nota e inserindo o link markdown no cursor.
     */
    private void installDropTarget() {
        TransferHandler handler = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (note == null) return false;
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                        || support.isDataFlavorSupported(DataFlavor.imageFlavor)
                        || support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                Transferable t = support.getTransferable();
                try {
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<java.io.File> files =
                                (List<java.io.File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        for (java.io.File f : files) attachFile(f.toPath());
                        return true;
                    }
                    if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        return pasteImage((java.awt.Image) t.getTransferData(DataFlavor.imageFlavor));
                    }
                    if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        content.replaceSelection((String) t.getTransferData(DataFlavor.stringFlavor));
                        return true;
                    }
                } catch (Exception ex) {
                    Log.warn(EditorPanel.class, "Falha a receber conteúdo largado ou colado", ex);
                }
                return false;
            }
        };
        content.setTransferHandler(handler);
        content.setDragEnabled(true);
    }

    /**
     * Guarda a imagem da área de transferência em {@code data/attachments} e insere
     * o link markdown. Devolve {@code true} se a imagem foi mesmo anexada.
     */
    private boolean pasteImage(java.awt.Image image) {
        if (note == null || image == null) return false;
        try {
            java.awt.image.BufferedImage buffered;
            if (image instanceof java.awt.image.BufferedImage b) {
                buffered = b;
            } else {
                buffered = new java.awt.image.BufferedImage(
                        image.getWidth(null), image.getHeight(null),
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = buffered.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
            Path temp = Files.createTempFile("jotes-colado-", ".png");
            javax.imageio.ImageIO.write((RenderedImage) buffered, "png", temp.toFile());
            attachFile(temp);
            Files.deleteIfExists(temp);
            return true;
        } catch (Exception ex) {
            Log.warn(EditorPanel.class, "Falha a colar imagem", ex);
            return false;
        }
    }

    /** Cola: imagem da área de transferência se houver uma, senão o comportamento normal. */
    private void pasteFromClipboard() {
        try {
            Transferable t = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)
                    && pasteImage((java.awt.Image) t.getTransferData(DataFlavor.imageFlavor))) {
                return;
            }
        } catch (Exception ex) {
            Log.warn(EditorPanel.class, "Falha a ler a área de transferência", ex);
        }
        content.paste();
    }

    /** Ecrã de quando não há nota aberta: ícone, título e as ações mais prováveis. */
    private static final class EmptyState extends JComponent {
        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            Icons.paintScaled(g2, Icons.NOTE, Theme.SEPARATOR, cx - 32, cy - 96, 64);

            g2.setFont(Theme.font(Font.BOLD, 17));
            g2.setColor(Theme.TEXT_DIM);
            drawCentered(g2, "Nenhuma nota aberta", cx, cy - 10);

            g2.setFont(Theme.font(Font.PLAIN, 12.5f));
            drawCentered(g2, "Ctrl+N cria uma nota    ·    Ctrl+K procura tudo", cx, cy + 16);
            drawCentered(g2, "Ctrl+D abre a nota de hoje    ·    Ctrl+/ mostra os atalhos", cx, cy + 38);
            g2.dispose();
        }

        private void drawCentered(java.awt.Graphics2D g2, String text, int cx, int y) {
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, cx - fm.stringWidth(text) / 2, y);
        }
    }

    private void updateMeta() {
        if (note == null) {
            onStatsChanged.accept(null);
            return;
        }
        String text = content.getText();
        onStatsChanged.accept(StatusBar.Stats.of(text, note.getUpdatedAt()));
        String trimmed = text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int chars = text.length();
        String counts = words + " palavras · " + chars + " caracteres";
        if (note.getUpdatedAt() != null) {
            meta.setText(META_FMT.format(note.getUpdatedAt()) + "   ·   " + counts);
        } else {
            meta.setText(counts);
        }
    }
}
