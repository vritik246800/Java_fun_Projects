package jotes.ui.editor;

import jotes.model.Attachment;
import jotes.ui.Theme;
import jotes.ui.anim.FadeTransition;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.List;

/**
 * Faixa de "chips" com os anexos da nota.
 * Clique no nome abre o ficheiro; o botão "×" remove-o (confirmação a cargo do listener).
 * Fica invisível quando não há anexos.
 */
public class AttachmentsPanel extends JPanel {

    /** Callbacks de interação com um anexo. */
    public interface Listener {
        void onOpen(Attachment a);
        void onDelete(Attachment a);
    }

    private final Listener listener;
    // chips ficam num painel interior com alfa, para o fade-in a cada nova lista
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final FadeTransition.FadePanel chipsFade = FadeTransition.wrap(chips);

    public AttachmentsPanel(Listener listener) {
        super(new BorderLayout());
        this.listener = listener;
        setOpaque(false);
        chips.setOpaque(false);
        add(chipsFade, BorderLayout.CENTER);
        setVisible(false);
    }

    /** Substitui a lista de chips; esconde a faixa se a lista for vazia. */
    public void setAttachments(List<Attachment> attachments) {
        chips.removeAll();
        if (attachments != null) {
            for (Attachment a : attachments) chips.add(chip(a));
        }
        boolean show = attachments != null && !attachments.isEmpty();
        setVisible(show);
        revalidate();
        repaint();
        if (show) FadeTransition.fadeIn(chipsFade, Duration.ofMillis(200));
    }

    private JComponent chip(Attachment a) {
        Theme.RoundedPanel chip = new Theme.RoundedPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chip.setCornerRadius(10);
        chip.setFill(Theme.CARD_BG);
        Border normal = BorderFactory.createCompoundBorder(
                Theme.roundedBorder(Theme.SEPARATOR, 1, 10), BorderFactory.createEmptyBorder(1, 6, 1, 3));
        Border hover = BorderFactory.createCompoundBorder(
                Theme.roundedBorder(Theme.ACCENT, 1, 10), BorderFactory.createEmptyBorder(1, 6, 1, 3));
        chip.setBorder(normal);

        JLabel name = new JLabel(a.getFilename() + "  (" + humanSize(a.getSizeBytes()) + ")");
        name.setFont(Theme.font(Font.PLAIN, 11));
        name.setForeground(Theme.TEXT);
        name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        name.setToolTipText("Abrir anexo");
        name.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) listener.onOpen(a);
            }
            @Override public void mouseEntered(MouseEvent e) {
                chip.setBorder(hover);
                name.setForeground(Theme.ACCENT);
            }
            @Override public void mouseExited(MouseEvent e) {
                chip.setBorder(normal);
                name.setForeground(Theme.TEXT);
            }
        });

        JButton remove = new JButton("×");
        Theme.styleToolbarButton(remove);
        remove.setFont(Theme.font(Font.BOLD, 12));
        remove.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        remove.setFocusable(false);
        remove.setToolTipText("Remover anexo");
        remove.addActionListener(e -> listener.onDelete(a));

        chip.add(name);
        chip.add(remove);
        return chip;
    }

    static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / 1048576.0);
    }
}
