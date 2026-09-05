package jotes.ui;

import jotes.model.Note;
import jotes.services.ReminderService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Lembretes de uma nota: lista os que estão por disparar e permite agendar novos,
 * seja por atalho ("daqui a 1 hora", "amanhã de manhã") ou por data e hora escritas.
 */
public class ReminderDialog extends JDialog {

    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    /** Atalhos rápidos de agendamento. */
    private static final String[] PRESET_LABELS = {
            "Escolher data e hora…", "Daqui a 15 minutos", "Daqui a 1 hora",
            "Daqui a 3 horas", "Amanhã às 9h", "Próxima semana"};

    private final ReminderService reminders;
    private final Note note;

    private final DefaultListModel<ReminderService.Reminder> model = new DefaultListModel<>();
    private final JList<ReminderService.Reminder> list = new JList<>(model);
    private final JComboBox<String> presets = new JComboBox<>(PRESET_LABELS);
    private final JTextField when = new Theme.PlaceholderField("dd/mm/aaaa hh:mm");
    private final JTextField message = new Theme.PlaceholderField("Mensagem (opcional)");

    public ReminderDialog(Window owner, ReminderService reminders, Note note) {
        super(owner, "Lembretes — " + note.displayTitle(), ModalityType.APPLICATION_MODAL);
        this.reminders = reminders;
        this.note = note;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 460);
        setLocationRelativeTo(owner);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.LIST_BG);
        list.setForeground(Theme.TEXT);
        list.setSelectionBackground(Theme.SELECTION);
        list.setSelectionForeground(Theme.TEXT);
        list.setFont(Theme.font(Font.PLAIN, 13));
        list.setCellRenderer((l, value, index, selected, focus) -> {
            JLabel label = new JLabel(DISPLAY.format(value.remindAt())
                    + (value.message().isBlank() ? "" : "  —  " + value.message()));
            label.setOpaque(true);
            label.setBackground(selected ? Theme.SELECTION : Theme.LIST_BG);
            label.setForeground(Theme.TEXT);
            label.setFont(Theme.font(Font.PLAIN, 13));
            label.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            return label;
        });

        presets.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        presets.addActionListener(e -> {
            Instant target = presetInstant(presets.getSelectedIndex());
            when.setEnabled(target == null);
            if (target != null) {
                when.setText(INPUT.format(LocalDateTime.ofInstant(target, ZoneId.systemDefault())));
            }
        });
        when.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton add = new JButton("Agendar");
        Theme.stylePrimaryButton(add);
        add.addActionListener(e -> addReminder());

        JButton remove = new JButton("Remover");
        Theme.styleButton(remove);
        remove.addActionListener(e -> {
            ReminderService.Reminder selected = list.getSelectedValue();
            if (selected == null) return;
            try {
                reminders.delete(selected.id());
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton close = new JButton("Fechar");
        Theme.styleButton(close);
        close.addActionListener(e -> dispose());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        form.add(caption("Quando"));
        form.add(presets);
        form.add(Box.createVerticalStrut(8));
        form.add(when);
        form.add(Box.createVerticalStrut(8));
        form.add(caption("Mensagem"));
        form.add(message);
        form.add(Box.createVerticalStrut(10));

        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        formButtons.setOpaque(false);
        formButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        formButtons.add(add);
        form.add(formButtons);

        JScrollPane scroll = new JScrollPane(list);
        Theme.styleScrollPane(scroll);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bottom.add(remove);
        bottom.add(close);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        content.add(form, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JPanel.WHEN_IN_FOCUSED_WINDOW);

        presets.setSelectedIndex(2); // "Daqui a 1 hora" é o caso mais comum
        reload();
    }

    private JLabel caption(String text) {
        JLabel label = new JLabel(text.toUpperCase(java.util.Locale.ROOT));
        label.setFont(Theme.font(Font.BOLD, 10));
        label.setForeground(Theme.TEXT_DIM);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        return label;
    }

    /** Instante do atalho escolhido, ou {@code null} para "escolher data e hora". */
    private static Instant presetInstant(int index) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = switch (index) {
            case 1 -> now.plusMinutes(15);
            case 2 -> now.plusHours(1);
            case 3 -> now.plusHours(3);
            case 4 -> now.plusDays(1).withHour(9).withMinute(0);
            case 5 -> now.plusWeeks(1).withHour(9).withMinute(0);
            default -> null;
        };
        return target == null ? null : target.atZone(ZoneId.systemDefault()).toInstant();
    }

    private void addReminder() {
        Instant target;
        try {
            LocalDateTime parsed = LocalDateTime.parse(when.getText().trim(), INPUT);
            target = parsed.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Data inválida. Usa o formato dd/mm/aaaa hh:mm.",
                    "Lembrete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (target.isBefore(Instant.now().minus(Duration.ofMinutes(1)))) {
            JOptionPane.showMessageDialog(this, "Essa data já passou.",
                    "Lembrete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            reminders.add(note.getId(), target, message.getText().trim());
            message.setText("");
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reload() {
        model.clear();
        try {
            List<ReminderService.Reminder> pending = reminders.pendingFor(note.getId());
            for (ReminderService.Reminder r : pending) model.addElement(r);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
