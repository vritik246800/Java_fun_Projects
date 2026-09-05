package jotes.ui;

import jotes.model.Note;
import jotes.ui.anim.Colors;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/** Renderer de cartões de nota desenhado com Java2D (Graphics2D), estilo Apple Notes dark. */
public class NoteCardRenderer extends JComponent implements ListCellRenderer<Note> {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    /** Altura fixa de cada célula da lista (cartão + respiro à volta). */
    public static final int CELL_HEIGHT = 70;

    private Note note;
    private int index;
    private boolean selected;
    private int hoverIndex = -1;
    // Progresso (0..1) das animações de hover/seleção por índice de célula;
    // sem entrada, a célula pinta o estado de repouso (ver hoverProgress()/selectionProgress()).
    private final Map<Integer, Float> hoverAnim = new HashMap<>();
    private final Map<Integer, Float> selectionAnim = new HashMap<>();

    /** Define o índice da célula sob o rato (-1 = nenhuma), usado para o efeito hover. */
    public void setHoverIndex(int hoverIndex) { this.hoverIndex = hoverIndex; }

    /** Índice da célula atualmente sob o rato (-1 = nenhuma). */
    public int getHoverIndex() { return hoverIndex; }

    /** Regista o progresso da animação de hover de uma célula (0 = repouso, 1 = hover). */
    public void setHoverProgress(int index, float p) { hoverAnim.put(index, p); }

    /** Regista o progresso da animação de seleção de uma célula (0 = normal, 1 = selecionada). */
    public void setSelectionProgress(int index, float p) { selectionAnim.put(index, p); }

    /** Progresso de hover registado para a célula, ou -1 se não houver registo. */
    public float getHoverProgress(int index) { return hoverAnim.getOrDefault(index, -1f); }

    /** Progresso de seleção registado para a célula, ou -1 se não houver registo. */
    public float getSelectionProgress(int index) { return selectionAnim.getOrDefault(index, -1f); }

    /** Limpa os progressos registados (ex.: quando o modelo da lista é reconstruído). */
    public void clearProgress() { hoverAnim.clear(); selectionAnim.clear(); }

    /** Progresso de hover da célula atual: o registo animado ou, na falta dele, o estado de repouso. */
    private float hoverProgress() {
        float p = getHoverProgress(index);
        return p >= 0f ? p : (index == hoverIndex ? 1f : 0f);
    }

    /** Progresso de seleção da célula atual: o registo animado ou, na falta dele, o estado real. */
    private float selectionProgress() {
        float p = getSelectionProgress(index);
        return p >= 0f ? p : (selected ? 1f : 0f);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Note> list, Note value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        this.note = value;
        this.index = index;
        this.selected = isSelected;
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(260, CELL_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (note == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int r = Theme.RADIUS;

        // Área do cartão (respiro à volta, para o fundo LIST_BG da lista ficar visível)
        int cx = 6, cy = 3, cw = w - 12, ch = h - 6;

        // Fundo: CARD_BG em repouso, CARD_HOVER em hover — interpolado pelo progresso
        // da animação; a seleção (SELECTION + realce ACCENT_BG) é esbatida por cima,
        // e o contorno passa de SEPARATOR para ACCENT com o mesmo progresso.
        float hp = hoverProgress();
        float sp = selectionProgress();
        g2.setColor(hp <= 0f ? Theme.CARD_BG
                : hp >= 1f ? Theme.CARD_HOVER
                : Colors.lerp(Theme.CARD_BG, Theme.CARD_HOVER, hp));
        g2.fillRoundRect(cx, cy, cw, ch, r, r);
        if (sp > 0f) {
            Composite oldComposite = g2.getComposite();
            if (sp < 1f) g2.setComposite(AlphaComposite.SrcOver.derive(sp));
            g2.setColor(Theme.SELECTION);
            g2.fillRoundRect(cx, cy, cw, ch, r, r);
            g2.setColor(Theme.ACCENT_BG);
            g2.fillRoundRect(cx, cy, cw, ch, r, r);
            g2.setComposite(oldComposite);
        }
        g2.setColor(sp <= 0f ? Theme.borderColor()
                : sp >= 1f ? Theme.ACCENT
                : Colors.lerp(Theme.borderColor(), Theme.ACCENT, sp));
        g2.drawRoundRect(cx, cy, cw - 1, ch - 1, r, r);

        int textX = cx + 14;
        int maxTextW = cw - 26;
        int titleY = cy + 24;

        // Indicador de nota fixada: pequeno alfinete em ACCENT antes do título
        if (note.isPinned()) {
            g2.setColor(Theme.ACCENT);
            g2.fillOval(textX, titleY - 11, 8, 8);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(textX + 4, titleY - 3, textX + 4, titleY + 3);
            textX += 14;
            maxTextW -= 14;
        }

        // Título em negrito
        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.font(Font.BOLD, 14f));
        g2.drawString(clip(note.displayTitle(), g2, maxTextW), textX, titleY);

        // Segunda linha: data + pré-visualização, em texto esbatido
        int lineY = titleY + 22;
        g2.setColor(Theme.TEXT_DIM);
        g2.setFont(Theme.font(Font.PLAIN, 12f));
        String date = DATE_FMT.format(note.getUpdatedAt());
        g2.drawString(date, cx + 14, lineY);
        int dateW = g2.getFontMetrics().stringWidth(date);
        String snippet = note.snippet();
        if (snippet != null && !snippet.isEmpty()) {
            int snippetX = cx + 14 + dateW + 10;
            g2.drawString(clip(snippet, g2, cx + cw - 12 - snippetX), snippetX, lineY);
        }

        g2.dispose();
    }

    /** Corta o texto com reticências para caber em {@code maxWidth} píxeis. */
    private String clip(String s, Graphics2D g2, int maxWidth) {
        if (s == null || maxWidth <= 0) return "";
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(s) <= maxWidth) return s;
        String dots = "…";
        while (!s.isEmpty() && fm.stringWidth(s + dots) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + dots;
    }
}
