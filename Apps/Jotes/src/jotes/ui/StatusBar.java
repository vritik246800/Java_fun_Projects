package jotes.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Barra inferior com as contagens da nota aberta: palavras, caracteres, tempo de
 * leitura estimado e última modificação. À direita mostra o contexto da vista
 * (quantas notas estão listadas).
 */
public class StatusBar extends JPanel {

    /** Velocidade de leitura usada na estimativa (palavras por minuto). */
    private static final int WORDS_PER_MINUTE = 220;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    /** Contagens de uma nota. */
    public record Stats(int words, int chars, Instant updatedAt) {

        /** Conta palavras e caracteres de um texto markdown (sem o interpretar). */
        public static Stats of(String text, Instant updatedAt) {
            String trimmed = text == null ? "" : text.trim();
            int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
            return new Stats(words, text == null ? 0 : text.length(), updatedAt);
        }

        /** Tempo de leitura estimado, arredondado para cima com mínimo de 1 min. */
        public Duration readingTime() {
            return Duration.ofMinutes(Math.max(1, (words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE));
        }
    }

    private final JLabel left = new JLabel(" ");
    private final JLabel right = new JLabel(" ");

    public StatusBar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(Theme.SIDEBAR_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));

        left.setFont(Theme.font(Font.PLAIN, 11));
        left.setForeground(Theme.TEXT_DIM);
        right.setFont(Theme.font(Font.PLAIN, 11));
        right.setForeground(Theme.TEXT_DIM);

        add(left);
        add(Box.createHorizontalGlue());
        add(right);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (left == null) return; // chamado pelo construtor do JPanel
        setBackground(Theme.SIDEBAR_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        left.setForeground(Theme.TEXT_DIM);
        right.setForeground(Theme.TEXT_DIM);
    }

    /** Mostra as contagens da nota aberta; {@code null} limpa a zona esquerda. */
    public void setStats(Stats stats) {
        if (stats == null) {
            left.setText("Nenhuma nota aberta");
            return;
        }
        long minutes = stats.readingTime().toMinutes();
        StringBuilder sb = new StringBuilder();
        sb.append(stats.words()).append(stats.words() == 1 ? " palavra" : " palavras");
        sb.append("  ·  ").append(stats.chars()).append(" caracteres");
        sb.append("  ·  ").append(minutes).append(" min de leitura");
        if (stats.updatedAt() != null) {
            sb.append("  ·  modificada ").append(FMT.format(stats.updatedAt()));
        }
        left.setText(sb.toString());
    }

    /** Contexto da lista à direita, ex. {@code "12 notas em Trabalho"}. */
    public void setContext(String text) {
        right.setText(text == null ? " " : text);
    }
}
