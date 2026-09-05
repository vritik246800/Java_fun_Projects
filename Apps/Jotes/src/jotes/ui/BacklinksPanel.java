package jotes.ui;

import jotes.db.LinkRepository;
import jotes.model.Note;
import jotes.ui.anim.FadeTransition;
import jotes.ui.anim.SmoothScroll;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Painel de ligações da nota atual: "Referências" (notas que apontam para esta)
 * e "Ligações" (notas para as quais esta aponta). Clique num item abre a nota.
 */
public class BacklinksPanel extends JPanel {

    private final LinkRepository linkRepo;
    private final JPanel content = new JPanel();
    // wrapper com alfa para o fade-in sempre que o conteúdo é substituído
    private final FadeTransition.FadePanel contentFade = FadeTransition.wrap(content);
    private final JLabel errorLabel = new JLabel();
    private Consumer<Long> onOpenNote = id -> {};

    public BacklinksPanel(LinkRepository linkRepo) {
        super(new BorderLayout());
        this.linkRepo = linkRepo;
        setBackground(Theme.BG);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(true);
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setFont(Theme.font(Font.PLAIN, 12));
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        JScrollPane scroll = new JScrollPane(contentFade);
        Theme.styleScrollPane(scroll);
        SmoothScroll.install(scroll);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(200, 400));

        clear();
    }

    /** Regista o callback chamado com o id da nota quando o utilizador clica num item. */
    public void setOnOpenNote(Consumer<Long> listener) {
        this.onOpenNote = listener;
    }

    /** Re-aplica as cores do tema quando a paleta muda (chamado por updateComponentTreeUI). */
    @Override
    public void updateUI() {
        super.updateUI();
        if (content == null) return; // chamado pelo construtor do JPanel
        setBackground(Theme.BG);
        content.setBackground(Theme.BG);
        errorLabel.setForeground(Theme.DANGER);
    }

    /** Preenche as secções "Referências" e "Ligações" para a nota indicada. */
    public void showNote(Note note) {
        List<Note> refs = List.of();
        List<Note> links = List.of();
        String error = "";
        try {
            refs = linkRepo.backlinksOf(note.getId());
            links = linkRepo.targetsOf(note.getId());
        } catch (SQLException ex) {
            error = "Erro ao carregar ligações.";
        }
        rebuild(refs, links, error);
    }

    /** Esvazia as duas secções. */
    public void clear() {
        rebuild(List.of(), List.of(), "");
    }

    private void rebuild(List<Note> refs, List<Note> links, String error) {
        content.removeAll();
        if (!error.isEmpty()) {
            errorLabel.setText(error);
            content.add(errorLabel);
            content.add(Box.createVerticalStrut(8));
        }
        addSection("Referências", refs);
        content.add(Box.createVerticalStrut(12));
        addSection("Ligações", links);
        content.revalidate();
        content.repaint();
        // sem ligações nenhumas, o painel esconde-se para dar espaço ao editor
        boolean empty = refs.isEmpty() && links.isEmpty() && error.isEmpty();
        if (empty == isVisible()) {
            setVisible(!empty);
            Container parent = getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        }
        if (!empty) FadeTransition.fadeIn(contentFade, Duration.ofMillis(200));
    }

    private void addSection(String title, List<Note> notes) {
        JLabel header = new JLabel(title);
        header.setFont(Theme.font(Font.BOLD, 11f));
        header.setForeground(Theme.TEXT_DIM);
        header.setAlignmentX(LEFT_ALIGNMENT);
        content.add(header);
        content.add(Box.createVerticalStrut(4));
        if (notes.isEmpty()) {
            JLabel empty = new JLabel("—");
            empty.setFont(Theme.font(Font.PLAIN, 13));
            empty.setForeground(Theme.TEXT_DIM);
            empty.setAlignmentX(LEFT_ALIGNMENT);
            content.add(empty);
            return;
        }
        for (Note n : notes) content.add(linkLabel(n));
    }

    /** Etiqueta clicável com aspeto de link (cor de acento, cursor de mão). */
    private JLabel linkLabel(Note note) {
        JLabel label = new JLabel(note.displayTitle());
        label.setFont(Theme.font(Font.PLAIN, 13));
        label.setForeground(Theme.accent());
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onOpenNote.accept(note.getId());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(Theme.TEXT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(Theme.accent());
            }
        });
        return label;
    }
}
