package jotes.ui;

import jotes.db.FolderRepository;
import jotes.db.NoteRepository;
import jotes.db.TagRepository;
import jotes.model.Folder;
import jotes.model.Note;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Easing;
import jotes.ui.anim.SmoothScroll;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Painel do meio (tema G2, estilo Apple Notes): pesquisa + lista de notas em cartões. */
public class NotesListPanel extends JPanel {

    private static final String ALL_TAGS = "Todas as tags";

    /** Duração das animações de hover dos cartões. */
    private static final Duration HOVER_ANIM = Duration.ofMillis(140);
    /** Duração das animações de seleção dos cartões. */
    private static final Duration SELECT_ANIM = Duration.ofMillis(180);

    private final NoteRepository noteRepo;
    private final FolderRepository folderRepo;
    private final Theme.PlaceholderField search = new Theme.PlaceholderField("Pesquisar notas…");
    private final NoteCardRenderer cardRenderer = new NoteCardRenderer();
    private final JComboBox<String> tagCombo = new JComboBox<>(new String[]{ALL_TAGS});
    private JButton newNote;
    private JButton sortButton;
    private final DefaultListModel<Note> model = new DefaultListModel<>();
    private final JList<Note> list = new JList<>(model) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (model.isEmpty()) paintEmptyState((Graphics2D) g, getWidth());
        }
    };

    /** Notas carregadas de cada vez; o resto entra ao chegar ao fim do scroll. */
    private static final int PAGE_SIZE = 200;

    private NoteRepository.Query query = NoteRepository.Query.all();
    private String tagFilter;
    private boolean updatingTags;
    private int loaded;
    private int total;
    private boolean loadingMore;

    // Animações dos cartões: animadores em curso por índice e última seleção conhecida.
    private final Map<Integer, Animator> hoverAnims = new HashMap<>();
    private final Map<Integer, Animator> selectionAnims = new HashMap<>();
    private int lastSelectedIndex = -1;
    private boolean rebuilding;

    private TagRepository tagRepo;

    private Consumer<Note> onNoteSelected = n -> {};
    private Consumer<Long> onNoteDeleted = id -> {};
    private Consumer<Note> onNoteCreated = n -> {};
    private Consumer<Note> onNoteRenamed = n -> {};
    private Consumer<Note> onNoteTrashed = n -> {};

    public NotesListPanel(NoteRepository noteRepo, FolderRepository folderRepo) {
        super(new BorderLayout());
        this.noteRepo = noteRepo;
        this.folderRepo = folderRepo;
        setBackground(Theme.LIST_BG);

        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, search.getPreferredSize().height));
        Timer searchDebounce = new Timer(300, e -> refresh());
        searchDebounce.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchDebounce.restart(); }
            public void removeUpdate(DocumentEvent e) { searchDebounce.restart(); }
            public void changedUpdate(DocumentEvent e) { searchDebounce.restart(); }
        });

        newNote = new JButton("+ Nota");
        Theme.stylePrimaryButton(newNote);
        newNote.setToolTipText("Nova nota (Ctrl+N)");
        newNote.addActionListener(e -> createNote());

        sortButton = new JButton("Ordenar");
        Theme.styleButton(sortButton);
        sortButton.setToolTipText("Ordenar notas");
        sortButton.addActionListener(e ->
                buildSortMenu().show(sortButton, 0, sortButton.getHeight()));

        tagCombo.setToolTipText("Filtrar por tag");
        tagCombo.setFont(Theme.font(Font.PLAIN, 13));
        tagCombo.setBackground(Theme.CARD_BG);
        tagCombo.setForeground(Theme.TEXT);
        tagCombo.setMaximumSize(new Dimension(150, tagCombo.getPreferredSize().height));
        tagCombo.addActionListener(e -> {
            if (updatingTags) return;
            Object sel = tagCombo.getSelectedItem();
            tagFilter = (sel == null || ALL_TAGS.equals(sel)) ? null : sel.toString();
            refresh();
        });

        // barra em duas linhas: pesquisa por cima (largura total), controlos por baixo
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        search.setAlignmentX(LEFT_ALIGNMENT);
        top.add(search);
        top.add(Box.createVerticalStrut(8));

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setOpaque(false);
        controls.setAlignmentX(LEFT_ALIGNMENT);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, controls.getPreferredSize().height));
        controls.add(sortButton);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(tagCombo);
        controls.add(Box.createHorizontalGlue());
        controls.add(newNote);
        top.add(controls);
        add(top, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.LIST_BG);
        list.setCellRenderer(cardRenderer);
        list.setFixedCellHeight(NoteCardRenderer.CELL_HEIGHT);
        list.setLayoutOrientation(JList.VERTICAL);
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Note n = list.getSelectedValue();
            if (n != null) onNoteSelected.accept(n);
        });
        list.addListSelectionListener(e -> animateSelectionChange());
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { maybePopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { maybePopup(e); }
            @Override
            public void mouseMoved(MouseEvent e) { updateHover(e.getPoint()); }
            @Override
            public void mouseExited(MouseEvent e) { updateHover(null); }
            private void maybePopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) list.setSelectedIndex(idx);
                if (list.getSelectedValue() != null) buildPopup().show(list, e.getX(), e.getY());
            }
            private void updateHover(Point p) {
                int idx = -1;
                if (p != null) {
                    idx = list.locationToIndex(p);
                    if (idx >= 0 && !list.getCellBounds(idx, idx).contains(p)) idx = -1;
                }
                if (idx != cardRenderer.getHoverIndex()) {
                    int old = cardRenderer.getHoverIndex();
                    cardRenderer.setHoverIndex(idx);
                    animateHover(old, 0f);
                    animateHover(idx, 1f);
                }
            }
        };
        list.addMouseListener(mouseHandler);
        list.addMouseMotionListener(mouseHandler);
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "apagarNota");
        list.getActionMap().put("apagarNota", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Note n = list.getSelectedValue();
                if (n != null) deleteNote(n);
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        Theme.styleScrollPane(scroll);
        SmoothScroll.install(scroll);
        // paginação: ao chegar perto do fim carrega a página seguinte
        scroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            JScrollBar bar = (JScrollBar) e.getAdjustable();
            if (bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - NoteCardRenderer.CELL_HEIGHT * 3) {
                loadMore();
            }
        });
        list.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Estado vazio da lista: ícone, título e sugestão adaptados à vista atual
     * (pesquisa sem resultados, lixeira vazia, pasta sem notas…).
     */
    private void paintEmptyState(Graphics2D g, int width) {
        String icon;
        String heading;
        String hint;
        if (!search.getText().isBlank()) {
            icon = Icons.SEARCH;
            heading = "Nada encontrado";
            hint = "Tenta outras palavras ou limpa a pesquisa";
        } else if (query.trash()) {
            icon = Icons.TRASH;
            heading = "Lixeira vazia";
            hint = "As notas apagadas aparecem aqui";
        } else if (query.archived()) {
            icon = Icons.ARCHIVE;
            heading = "Sem notas arquivadas";
            hint = "Arquiva notas pelo menu de contexto";
        } else if (query.pinnedOnly()) {
            icon = Icons.PIN;
            heading = "Sem notas fixadas";
            hint = "Fixa uma nota pelo menu de contexto";
        } else {
            icon = Icons.NOTE;
            heading = "Ainda sem notas";
            hint = "Cria a primeira com Ctrl+N";
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cx = width / 2;
        Icons.paintScaled(g2, icon, Theme.SEPARATOR, cx - 20, 48, 40);

        g2.setFont(Theme.font(Font.BOLD, 14));
        g2.setColor(Theme.TEXT_DIM);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(heading, cx - fm.stringWidth(heading) / 2, 112);

        g2.setFont(Theme.font(Font.PLAIN, 12));
        fm = g2.getFontMetrics();
        g2.drawString(hint, cx - fm.stringWidth(hint) / 2, 132);
        g2.dispose();
    }

    public void setOnNoteSelected(Consumer<Note> listener) { this.onNoteSelected = listener; }

    /** Chamado depois de renomear uma nota pelo menu de contexto. */
    public void setOnNoteRenamed(Consumer<Note> listener) { this.onNoteRenamed = listener; }

    /** Chamado quando uma nota vai para a lixeira (para oferecer "Anular"). */
    public void setOnNoteTrashed(Consumer<Note> listener) { this.onNoteTrashed = listener; }
    public void setOnNoteDeleted(Consumer<Long> listener) { this.onNoteDeleted = listener; }

    /** Re-aplica as cores do tema quando a paleta muda (chamado por updateComponentTreeUI). */
    @Override
    public void updateUI() {
        super.updateUI();
        if (newNote == null) return; // chamado pelo construtor do JPanel
        setBackground(Theme.LIST_BG);
        list.setBackground(Theme.LIST_BG);
        tagCombo.setBackground(Theme.CARD_BG);
        tagCombo.setForeground(Theme.TEXT);
        search.setBackground(Theme.FIELD_BG);
        search.setForeground(Theme.TEXT);
        search.setCaretColor(Theme.TEXT);
        search.setSelectionColor(Theme.TEXT_SELECTION);
        search.setSelectedTextColor(Theme.TEXT);
        Theme.stylePrimaryButton(newNote);
        Theme.styleButton(sortButton);
    }

    /** Anima a transição de seleção entre a célula anterior e a atual. */
    private void animateSelectionChange() {
        if (rebuilding) return;
        int idx = list.getSelectedIndex();
        if (idx == lastSelectedIndex) return;
        int old = lastSelectedIndex;
        lastSelectedIndex = idx;
        if (old >= 0) animateSelection(old, 0f);
        if (idx >= 0) animateSelection(idx, 1f);
    }

    /** Anima o hover de uma célula para o valor alvo (0 = repouso, 1 = hover). */
    private void animateHover(int index, float target) {
        if (index < 0) return;
        float start = cardRenderer.getHoverProgress(index);
        if (start < 0f) start = target == 0f ? 1f : 0f; // sem registo: parte do estado de repouso
        animateCell(hoverAnims, index, start, target, HOVER_ANIM, true);
    }

    /** Anima a seleção de uma célula para o valor alvo (0 = normal, 1 = selecionada). */
    private void animateSelection(int index, float target) {
        if (index < 0) return;
        float start = cardRenderer.getSelectionProgress(index);
        if (start < 0f) start = target == 0f ? 1f : 0f;
        animateCell(selectionAnims, index, start, target, SELECT_ANIM, false);
    }

    /** Interpola o progresso de uma célula entre {@code start} e {@code target}, repintando-a. */
    private void animateCell(Map<Integer, Animator> running, int index, float start, float target,
                             Duration duration, boolean hover) {
        Animator prev = running.remove(index);
        if (prev != null) prev.cancel();
        // Regista já o valor inicial para a célula não pintar o estado final antes do 1.º pulso.
        setProgress(hover, index, start);
        if (start == target) {
            repaintCell(index);
            return;
        }
        Animator a = Animator.animate(duration, Easing.EASE_OUT,
                v -> {
                    setProgress(hover, index, (float) (start + (target - start) * v));
                    repaintCell(index);
                },
                () -> running.remove(index));
        if (a.isRunning()) running.put(index, a);
    }

    private void setProgress(boolean hover, int index, float p) {
        if (hover) cardRenderer.setHoverProgress(index, p);
        else cardRenderer.setSelectionProgress(index, p);
    }

    /** Repinta apenas a célula indicada (nada se o índice já não existir no modelo). */
    private void repaintCell(int index) {
        if (index < 0 || index >= model.getSize()) return;
        Rectangle r = list.getCellBounds(index, index);
        if (r != null) list.repaint(r);
    }

    /** Cancela as animações em curso e repõe o estado de repouso dos cartões. */
    private void cancelCellAnimations() {
        hoverAnims.values().forEach(Animator::cancel);
        hoverAnims.clear();
        selectionAnims.values().forEach(Animator::cancel);
        selectionAnims.clear();
        cardRenderer.clearProgress();
    }

    /** Chamado depois de criar uma nota nova (ex.: para focar o título no editor). */
    public void setOnNoteCreated(Consumer<Note> listener) { this.onNoteCreated = listener; }

    public void showNotes(SidebarPanel.Selection sel) {
        query = NoteRepository.Query.all()
                .withFolder(sel.folderId())
                .withPinnedOnly(sel.pinnedOnly())
                .withArchived(sel.archived())
                .withTrash(sel.trash())
                .withSort(query.sort());
        sidebarTag = sel.tag();
        search.setText("");
        refresh();
    }

    /** Tag imposta pela sidebar (soma-se ao filtro do combo). */
    private String sidebarTag;

    /** {@code true} quando a vista atual é a lixeira. */
    public boolean isTrashView() {
        return query.trash();
    }

    public void refresh() {
        refreshTags();
        Long selectedId = list.getSelectedValue() != null ? list.getSelectedValue().getId() : null;
        // Durante a reconstrução do modelo as animações por índice são suprimidas:
        // os índices mudam e o estado final é sincronizado no fim, sem transições.
        rebuilding = true;
        cancelCellAnimations();
        try {
            NoteRepository.Query q = currentQuery();
            total = noteRepo.count(q);
            List<Note> notes = noteRepo.list(q.withPage(PAGE_SIZE, 0));
            loaded = notes.size();
            model.clear();
            for (Note n : notes) model.addElement(n);
            if (selectedId != null) {
                for (int i = 0; i < model.size(); i++) {
                    if (model.get(i).getId() == selectedId) {
                        list.setSelectedIndex(i);
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            showError(ex);
        } finally {
            rebuilding = false;
            lastSelectedIndex = list.getSelectedIndex();
        }
    }

    /** Query efetiva: vista da sidebar + tag do combo (ou da sidebar) + texto da pesquisa. */
    private NoteRepository.Query currentQuery() {
        String tag = tagFilter != null ? tagFilter : sidebarTag;
        return query.withTag(tag).withText(search.getText());
    }

    /**
     * Carrega a página seguinte quando ainda faltam notas. Chamado ao aproximar-se
     * do fim do scroll — evita construir milhares de células no arranque.
     */
    private void loadMore() {
        if (loadingMore || loaded >= total) return;
        loadingMore = true;
        try {
            List<Note> next = noteRepo.list(currentQuery().withPage(PAGE_SIZE, loaded));
            for (Note n : next) model.addElement(n);
            loaded += next.size();
        } catch (Exception ex) {
            showError(ex);
        } finally {
            loadingMore = false;
        }
    }

    private void createNote() {
        try {
            Note n = noteRepo.create(query.archived() || query.trash() ? null : query.folderId());
            refresh();
            selectNote(n.getId());
            onNoteCreated.accept(n);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Foca o campo de pesquisa e seleciona o texto atual. */
    public void focusSearch() {
        search.requestFocusInWindow();
        search.selectAll();
    }

    /** Cria uma nova nota (equivalente ao botão "+ Nota"). */
    public void requestNewNote() {
        createNote();
    }

    /** Seleciona (e torna visível) a nota com o id indicado, se estiver na lista atual. */
    public void selectNote(long id) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).getId() == id) {
                list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private JPopupMenu buildPopup() {
        Note n = list.getSelectedValue();
        JPopupMenu menu = new JPopupMenu();

        if (query.trash()) {
            JMenuItem restore = new JMenuItem("Restaurar");
            restore.addActionListener(e -> run(() -> noteRepo.restore(n.getId())));
            JMenuItem purge = new JMenuItem("Apagar definitivamente");
            purge.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(this,
                        "Apagar \"" + n.displayTitle() + "\" para sempre? Não há como recuperar.",
                        "Apagar definitivamente", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ok == JOptionPane.OK_OPTION) run(() -> {
                    noteRepo.purge(n.getId());
                    onNoteDeleted.accept(n.getId());
                });
            });
            menu.add(restore);
            menu.addSeparator();
            menu.add(purge);
            return menu;
        }

        JMenuItem rename = new JMenuItem("Renomear");
        rename.addActionListener(e -> {
            String newTitle = (String) JOptionPane.showInputDialog(this, "Novo título:", "Renomear nota",
                    JOptionPane.PLAIN_MESSAGE, null, null, n.getTitle());
            if (newTitle == null) return;
            run(() -> {
                n.setTitle(newTitle.trim());
                noteRepo.save(n);
                onNoteRenamed.accept(n);
            });
        });

        JMenuItem pin = new JMenuItem(n.isPinned() ? "Desafixar" : "Fixar");
        pin.addActionListener(e -> run(() -> {
            n.setPinned(!n.isPinned());
            noteRepo.updateFlags(n);
        }));

        JMenuItem duplicate = new JMenuItem("Duplicar");
        duplicate.addActionListener(e -> run(() -> {
            Note copy = noteRepo.duplicate(n);
            refresh();
            selectNote(copy.getId());
        }));

        JMenu moveTo = new JMenu("Mover para");
        JMenuItem noFolder = new JMenuItem("Sem pasta");
        noFolder.addActionListener(e -> run(() -> noteRepo.move(n.getId(), null)));
        moveTo.add(noFolder);
        try {
            for (Folder f : folderRepo.list()) {
                JMenuItem item = new JMenuItem(f.getName());
                item.addActionListener(e -> run(() -> noteRepo.move(n.getId(), f.getId())));
                moveTo.add(item);
            }
        } catch (Exception ex) {
            showError(ex);
        }

        JMenuItem archive = new JMenuItem(query.archived() ? "Restaurar" : "Arquivar");
        archive.addActionListener(e -> run(() -> {
            n.setArchived(!query.archived());
            n.setPinned(false);
            noteRepo.updateFlags(n);
        }));

        JMenuItem delete = new JMenuItem("Mover para a lixeira");
        delete.addActionListener(e -> deleteNote(n));

        menu.add(rename);
        menu.add(pin);
        menu.add(duplicate);
        menu.add(moveTo);
        menu.add(archive);
        menu.addSeparator();
        menu.add(delete);
        return menu;
    }

    /** Apaga a nota indicada após confirmação (usado pelo menu popup e pela tecla Delete). */
    private void deleteNote(Note n) {
        try {
            if (query.trash()) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "Apagar \"" + n.displayTitle() + "\" para sempre? Não há como recuperar.",
                        "Apagar definitivamente", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ok != JOptionPane.OK_OPTION) return;
                noteRepo.purge(n.getId());
            } else {
                // exclusão suave: sem diálogo, com desfazer no aviso
                noteRepo.delete(n.getId());
                onNoteTrashed.accept(n);
            }
            onNoteDeleted.accept(n.getId());
            refresh();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Injeta o repositório de tags partilhado (opcional; se não for injetado, o combo fica desativado). */
    public void setTagRepository(TagRepository tagRepo) {
        this.tagRepo = tagRepo;
        refresh();
    }

    private JPopupMenu buildSortMenu() {
        JPopupMenu menu = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();
        addSortItem(menu, group, "Última modificação", NoteRepository.SORT_UPDATED);
        addSortItem(menu, group, "Data de criação", NoteRepository.SORT_CREATED);
        addSortItem(menu, group, "Título (A–Z)", NoteRepository.SORT_TITLE);
        return menu;
    }

    private void addSortItem(JPopupMenu menu, ButtonGroup group, String label, String value) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, value.equals(query.sort()));
        item.addActionListener(e -> {
            query = query.withSort(value);
            refresh();
        });
        group.add(item);
        menu.add(item);
    }

    /** Recarrega a lista de tags no combo, preservando a seleção atual. */
    private void refreshTags() {
        TagRepository tr = tagRepo;
        if (tr == null) {
            tagCombo.setEnabled(false);
            return;
        }
        tagCombo.setEnabled(true);
        List<String> tags;
        try {
            tags = tr.listAll();
        } catch (Exception ex) {
            return; // não bloqueia a lista de notas por causa das tags
        }
        List<String> current = new ArrayList<>();
        for (int i = 1; i < tagCombo.getItemCount(); i++) current.add(tagCombo.getItemAt(i));
        if (current.equals(tags)) return; // sem alterações
        updatingTags = true;
        try {
            tagCombo.removeAllItems();
            tagCombo.addItem(ALL_TAGS);
            for (String t : tags) tagCombo.addItem(t);
            if (tagFilter != null && tags.contains(tagFilter)) {
                tagCombo.setSelectedItem(tagFilter);
            } else {
                tagFilter = null;
                tagCombo.setSelectedIndex(0);
            }
        } finally {
            updatingTags = false;
        }
    }

    private void run(SqlAction action) {
        try {
            action.run();
            refresh();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        jotes.util.Log.warn(NotesListPanel.class, "Operação sobre notas falhou", ex);
        Toast.error(this, ex.getMessage() == null ? "Operação falhou" : ex.getMessage());
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws Exception;
    }
}
