package jotes.ui;

import jotes.db.NoteRepository;
import jotes.model.Note;
import jotes.ui.anim.Animator;
import jotes.ui.anim.Easing;
import jotes.ui.anim.SmoothScroll;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

/**
 * Estatísticas de escrita: totais, notas criadas por semana nas últimas 12 semanas
 * e a sequência ("streak") de dias seguidos com pelo menos uma nota criada ou
 * modificada. As barras do gráfico sobem com uma animação curta.
 */
public class StatsDialog extends JDialog {

    /** Semanas mostradas no gráfico. */
    private static final int WEEKS = 12;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

    public StatsDialog(Window owner, NoteRepository noteRepo) {
        super(owner, "Estatísticas", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 620);
        setLocationRelativeTo(owner);

        List<Note> notes;
        try {
            notes = noteRepo.listAll();
        } catch (Exception ex) {
            notes = List.of();
        }
        notes.removeIf(Note::isDeleted);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        Totals totals = Totals.of(notes);
        body.add(tiles(totals));
        body.add(Box.createVerticalStrut(20));
        body.add(header("Notas criadas por semana"));
        body.add(new WeeklyChart(weekly(notes)));
        body.add(Box.createVerticalStrut(20));
        body.add(header("Sequência de escrita"));
        body.add(streakCard(totals));
        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        Theme.styleScrollPane(scroll);
        SmoothScroll.install(scroll);

        JButton close = new JButton("Fechar");
        Theme.stylePrimaryButton(close);
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setOpaque(false);
        buttons.add(close);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JPanel.WHEN_IN_FOCUSED_WINDOW);
    }

    // ---------------------------------------------------------------- dados

    /** Agregados calculados uma vez a partir de todas as notas. */
    private record Totals(int notes, long words, long chars, int tags, int currentStreak, int bestStreak,
                          LocalDate firstNote) {

        static Totals of(List<Note> notes) {
            long words = 0;
            long chars = 0;
            Set<String> tags = new HashSet<>();
            Set<LocalDate> activeDays = new HashSet<>();
            LocalDate first = null;

            for (Note n : notes) {
                String text = n.getContent().trim();
                if (!text.isEmpty()) words += text.split("\\s+").length;
                chars += n.getContent().length();
                tags.addAll(n.getTags());
                if (n.getCreatedAt() != null) {
                    LocalDate day = LocalDate.ofInstant(n.getCreatedAt(), ZoneId.systemDefault());
                    activeDays.add(day);
                    if (first == null || day.isBefore(first)) first = day;
                }
                if (n.getUpdatedAt() != null) {
                    activeDays.add(LocalDate.ofInstant(n.getUpdatedAt(), ZoneId.systemDefault()));
                }
            }
            return new Totals(notes.size(), words, chars, tags.size(),
                    currentStreak(activeDays), bestStreak(activeDays), first);
        }

        /** Dias seguidos até hoje (ou até ontem, se ainda não se escreveu hoje). */
        private static int currentStreak(Set<LocalDate> days) {
            LocalDate cursor = LocalDate.now();
            if (!days.contains(cursor)) cursor = cursor.minusDays(1);
            int streak = 0;
            while (days.contains(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            }
            return streak;
        }

        /** A maior sequência de sempre. */
        private static int bestStreak(Set<LocalDate> days) {
            int best = 0;
            for (LocalDate day : days) {
                if (days.contains(day.minusDays(1))) continue; // só arranca em inícios de sequência
                int run = 0;
                LocalDate cursor = day;
                while (days.contains(cursor)) {
                    run++;
                    cursor = cursor.plusDays(1);
                }
                best = Math.max(best, run);
            }
            return best;
        }
    }

    /** Contagem de notas criadas por semana, das mais antigas para as mais recentes. */
    private static List<int[]> weeklyCounts(List<Note> notes, LocalDate start) {
        TreeMap<Long, Integer> byWeek = new TreeMap<>();
        for (int i = 0; i < WEEKS; i++) byWeek.put((long) i, 0);
        for (Note n : notes) {
            if (n.getCreatedAt() == null) continue;
            LocalDate created = LocalDate.ofInstant(n.getCreatedAt(), ZoneId.systemDefault());
            long week = ChronoUnit.WEEKS.between(start, created);
            if (week >= 0 && week < WEEKS) byWeek.merge(week, 1, Integer::sum);
        }
        List<int[]> out = new ArrayList<>();
        for (var e : byWeek.entrySet()) out.add(new int[]{e.getKey().intValue(), e.getValue()});
        return out;
    }

    private List<Bar> weekly(List<Note> notes) {
        LocalDate start = LocalDate.now().minusWeeks(WEEKS - 1L)
                .with(java.time.DayOfWeek.MONDAY);
        List<Bar> bars = new ArrayList<>();
        for (int[] pair : weeklyCounts(notes, start)) {
            LocalDate weekStart = start.plusWeeks(pair[0]);
            bars.add(new Bar(DAY.format(weekStart), pair[1]));
        }
        return bars;
    }

    private record Bar(String label, int value) {}

    // ---------------------------------------------------------------- UI

    private JComponent header(String text) {
        JLabel label = new JLabel(text.toUpperCase(Locale.ROOT));
        label.setFont(Theme.font(Font.BOLD, 11));
        label.setForeground(Theme.TEXT_DIM);
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        wrap.add(label);
        return wrap;
    }

    private JComponent tiles(Totals t) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        row.add(tile(String.valueOf(t.notes()), "notas"));
        row.add(Box.createHorizontalStrut(10));
        row.add(tile(compact(t.words()), "palavras"));
        row.add(Box.createHorizontalStrut(10));
        row.add(tile(compact(t.chars()), "caracteres"));
        row.add(Box.createHorizontalStrut(10));
        row.add(tile(String.valueOf(t.tags()), "tags"));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        return row;
    }

    private JComponent tile(String value, String label) {
        Theme.RoundedPanel panel = new Theme.RoundedPanel(new BorderLayout());
        panel.setFill(Theme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel big = new JLabel(value);
        big.setFont(Theme.font(Font.BOLD, 22));
        big.setForeground(Theme.TEXT);

        JLabel small = new JLabel(label);
        small.setFont(Theme.font(Font.PLAIN, 11));
        small.setForeground(Theme.TEXT_DIM);

        panel.add(big, BorderLayout.CENTER);
        panel.add(small, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent streakCard(Totals t) {
        Theme.RoundedPanel panel = new Theme.RoundedPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setFill(Theme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        panel.add(line(t.currentStreak() == 0
                ? "Sem sequência ativa — escreve hoje para começar uma."
                : t.currentStreak() + (t.currentStreak() == 1 ? " dia seguido" : " dias seguidos") + " a escrever.",
                Font.BOLD, 15, Theme.TEXT));
        panel.add(Box.createVerticalStrut(6));
        panel.add(line("Melhor sequência: " + t.bestStreak()
                + (t.bestStreak() == 1 ? " dia" : " dias"), Font.PLAIN, 12, Theme.TEXT_DIM));
        if (t.firstNote() != null) {
            long days = ChronoUnit.DAYS.between(t.firstNote(), LocalDate.now()) + 1;
            panel.add(line("Primeira nota há " + days + (days == 1 ? " dia" : " dias")
                    + " (" + t.firstNote().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("pt"))
                    + ", " + t.firstNote() + ")", Font.PLAIN, 12, Theme.TEXT_DIM));
        }
        return panel;
    }

    private JLabel line(String text, int style, float size, java.awt.Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return label;
    }

    /** Números grandes em forma curta: {@code 12 400 → 12,4 k}. */
    private static String compact(long value) {
        if (value < 1000) return String.valueOf(value);
        if (value < 1_000_000) return String.format(Locale.ROOT, "%.1f k", value / 1000.0);
        return String.format(Locale.ROOT, "%.1f M", value / 1_000_000.0);
    }

    /** Gráfico de barras das notas por semana, com as barras a crescer na abertura. */
    private static final class WeeklyChart extends JComponent {
        private final List<Bar> bars;
        private float progress;

        WeeklyChart(List<Bar> bars) {
            this.bars = bars;
            setAlignmentX(LEFT_ALIGNMENT);
            Animator.animate(Duration.ofMillis(520), Easing.EASE_OUT, v -> {
                progress = (float) v;
                repaint();
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(560, 180);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 180);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int max = 1;
            for (Bar b : bars) max = Math.max(max, b.value());

            int bottom = getHeight() - 22;
            g2.setColor(Theme.SEPARATOR);
            g2.drawLine(0, bottom, getWidth(), bottom);

            g2.setFont(Theme.font(Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();
            int count = Math.max(1, bars.size());
            float slot = getWidth() / (float) count;
            float barWidth = Math.min(34, slot * 0.6f);

            for (int i = 0; i < bars.size(); i++) {
                Bar b = bars.get(i);
                float height = (bottom - 22) * (b.value() / (float) max) * progress;
                float x = i * slot + (slot - barWidth) / 2;

                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(Math.round(x), Math.round(bottom - height),
                        Math.round(barWidth), Math.round(Math.max(1, height)), 6, 6);

                if (b.value() > 0) {
                    g2.setColor(Theme.TEXT);
                    String value = String.valueOf(b.value());
                    g2.drawString(value, x + (barWidth - fm.stringWidth(value)) / 2,
                            bottom - height - 4);
                }
                g2.setColor(Theme.TEXT_DIM);
                g2.drawString(b.label(), x + (barWidth - fm.stringWidth(b.label())) / 2, bottom + 14);
            }
            g2.dispose();
        }
    }
}
