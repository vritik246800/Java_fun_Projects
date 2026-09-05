package jotes.ui;

import jotes.db.SettingsRepository;
import jotes.services.BackupService;
import jotes.services.SyncService;
import jotes.ui.anim.Animations;
import jotes.ui.anim.SmoothScroll;
import jotes.util.Log;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Definições da aplicação num só sítio: aparência (tema e animações), backup
 * automático agendado e sincronização por pasta — as três coisas que até aqui
 * só existiam em código ou nas definições da base de dados.
 */
public class SettingsDialog extends JDialog {

    private static final String[] INTERVAL_LABELS = {
            "Desligado", "A cada hora", "A cada 6 horas", "A cada 12 horas", "Diário", "Semanal"};
    private static final int[] INTERVAL_HOURS = {0, 1, 6, 12, 24, 24 * 7};

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final SettingsRepository settings;
    private final BackupService backupService;
    private final SyncService syncService;

    private final JCheckBox darkTheme = new JCheckBox("Tema escuro");
    private final JCheckBox reduceAnimations = new JCheckBox("Reduzir animações");
    private final JComboBox<String> backupInterval = new JComboBox<>(INTERVAL_LABELS);
    private final JSpinner backupKeep = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
    private final JLabel backupStatus = new JLabel(" ");
    private final JTextField syncFolder = new Theme.PlaceholderField("Pasta partilhada (Dropbox, OneDrive…)");

    private Runnable onThemeChanged = () -> {};

    public SettingsDialog(Window owner, SettingsRepository settings,
                          BackupService backupService, SyncService syncService) {
        super(owner, "Definições", ModalityType.APPLICATION_MODAL);
        this.settings = settings;
        this.backupService = backupService;
        this.syncService = syncService;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(560, 560);
        setLocationRelativeTo(owner);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        body.add(new SectionHeader("Aparência"));
        body.add(appearance());
        body.add(Box.createVerticalStrut(18));
        body.add(new SectionHeader("Backup automático"));
        body.add(backup());
        body.add(Box.createVerticalStrut(18));
        body.add(new SectionHeader("Sincronização"));
        body.add(sync());
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

        load();
    }

    /** Chamado quando o tema ou as animações mudam, para a janela principal se atualizar. */
    public void setOnThemeChanged(Runnable listener) {
        this.onThemeChanged = listener == null ? () -> {} : listener;
    }

    // ---------------------------------------------------------------- secções

    /** Cabeçalho de secção — rótulo em maiúsculas, alinhado à esquerda. */
    private static final class SectionHeader extends JPanel {
        SectionHeader(String title) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            JLabel label = new JLabel(title.toUpperCase(java.util.Locale.ROOT));
            label.setFont(Theme.font(Font.BOLD, 11));
            label.setForeground(Theme.TEXT_DIM);
            add(label);
            setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    private JPanel card(Component... children) {
        Theme.RoundedPanel panel = new Theme.RoundedPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setFill(Theme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : children) {
            if (c instanceof JPanel p) p.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(c);
        }
        return panel;
    }

    private JPanel row(Component... children) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : children) row.add(c);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return row;
    }

    private JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.PLAIN, 11));
        label.setForeground(Theme.TEXT_DIM);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        return label;
    }

    private void styleCheck(JCheckBox box) {
        box.setOpaque(false);
        box.setForeground(Theme.TEXT);
        box.setFont(Theme.font(Font.PLAIN, 13));
        box.setFocusPainted(false);
    }

    private JPanel appearance() {
        styleCheck(darkTheme);
        styleCheck(reduceAnimations);

        darkTheme.addActionListener(e -> {
            Theme.setDark(darkTheme.isSelected());
            save(SettingsRepository.UI_DARK, darkTheme.isSelected());
            onThemeChanged.run();
        });
        reduceAnimations.addActionListener(e -> {
            Animations.setEnabled(!reduceAnimations.isSelected());
            save(SettingsRepository.UI_REDUCE_ANIMATIONS, reduceAnimations.isSelected());
        });

        return card(row(darkTheme, Box.createHorizontalGlue()),
                row(reduceAnimations, Box.createHorizontalGlue()),
                hint("Reduzir animações desliga fades, slides e a física do grafo."));
    }

    private JPanel backup() {
        JLabel intervalLabel = new JLabel("Frequência:  ");
        intervalLabel.setForeground(Theme.TEXT);
        intervalLabel.setFont(Theme.font(Font.PLAIN, 13));
        backupInterval.setMaximumSize(new Dimension(200, 30));
        backupInterval.addActionListener(e ->
                save(SettingsRepository.BACKUP_INTERVAL_HOURS, INTERVAL_HOURS[backupInterval.getSelectedIndex()]));

        JLabel keepLabel = new JLabel("Manter:  ");
        keepLabel.setForeground(Theme.TEXT);
        keepLabel.setFont(Theme.font(Font.PLAIN, 13));
        backupKeep.setMaximumSize(new Dimension(80, 30));
        backupKeep.addChangeListener(e ->
                save(SettingsRepository.BACKUP_KEEP, (Integer) backupKeep.getValue()));

        JButton now = new JButton("Fazer backup agora");
        Theme.styleButton(now);
        now.addActionListener(e -> {
            try {
                Path created = backupService.createBackup("manual");
                backupService.pruneBackups((Integer) backupKeep.getValue());
                JOptionPane.showMessageDialog(this, "Backup criado em:\n" + created,
                        "Backup", JOptionPane.INFORMATION_MESSAGE);
                refreshBackupStatus();
            } catch (Exception ex) {
                Log.warn(SettingsDialog.class, "Backup manual falhou", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        backupStatus.setFont(Theme.font(Font.PLAIN, 11));
        backupStatus.setForeground(Theme.TEXT_DIM);
        backupStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        return card(row(intervalLabel, backupInterval, Box.createHorizontalGlue()),
                Box.createVerticalStrut(8),
                row(keepLabel, backupKeep, Box.createHorizontalGlue(), now),
                Box.createVerticalStrut(6),
                backupStatus,
                hint("Os backups ficam em «backups/» junto à base de dados."));
    }

    private JPanel sync() {
        syncFolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton choose = new JButton("Escolher…");
        Theme.styleButton(choose);
        choose.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Pasta de sincronização");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                syncFolder.setText(fc.getSelectedFile().getAbsolutePath());
                saveSyncFolder();
            }
        });

        JButton syncNow = new JButton("Sincronizar agora");
        Theme.styleButton(syncNow);
        syncNow.addActionListener(e -> {
            saveSyncFolder();
            try {
                int updated = syncService.syncNow();
                JOptionPane.showMessageDialog(this,
                        updated == 0 ? "Sincronizado. Nada de novo do outro lado."
                                : updated + " nota(s) atualizada(s) a partir da pasta.",
                        "Sincronização", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Log.warn(SettingsDialog.class, "Sincronização falhou", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        return card(row(syncFolder, Box.createHorizontalStrut(8), choose),
                Box.createVerticalStrut(8),
                row(Box.createHorizontalGlue(), syncNow),
                hint("Faz merge por uuid (ganha a versão mais recente). Não apaga notas."));
    }

    // ---------------------------------------------------------------- estado

    private void load() {
        try {
            darkTheme.setSelected(settings.getBoolean(SettingsRepository.UI_DARK, true));
            reduceAnimations.setSelected(
                    settings.getBoolean(SettingsRepository.UI_REDUCE_ANIMATIONS, false));
            int hours = settings.getInt(SettingsRepository.BACKUP_INTERVAL_HOURS, 0);
            backupInterval.setSelectedIndex(indexOfInterval(hours));
            backupKeep.setValue(settings.getInt(SettingsRepository.BACKUP_KEEP, BackupService.KEEP_BACKUPS));
            String folder = syncService.syncFolder();
            syncFolder.setText(folder == null ? "" : folder);
            refreshBackupStatus();
        } catch (Exception ex) {
            Log.warn(SettingsDialog.class, "Não foi possível ler as definições", ex);
        }
    }

    private void refreshBackupStatus() {
        try {
            long last = settings.getLong(SettingsRepository.BACKUP_LAST_AT, 0);
            backupStatus.setText(last == 0 ? "Ainda sem backup automático."
                    : "Último backup automático: " + FMT.format(Instant.ofEpochMilli(last)));
        } catch (Exception ex) {
            backupStatus.setText(" ");
        }
    }

    private static int indexOfInterval(int hours) {
        for (int i = 0; i < INTERVAL_HOURS.length; i++) {
            if (INTERVAL_HOURS[i] == hours) return i;
        }
        return 0;
    }

    private void saveSyncFolder() {
        try {
            syncService.setSyncFolder(syncFolder.getText().trim());
        } catch (Exception ex) {
            Log.warn(SettingsDialog.class, "Não foi possível guardar a pasta de sincronização", ex);
        }
    }

    private void save(String key, boolean value) {
        try {
            settings.set(key, value);
        } catch (Exception ex) {
            Log.warn(SettingsDialog.class, "Não foi possível guardar " + key, ex);
        }
    }

    private void save(String key, int value) {
        try {
            settings.set(key, value);
        } catch (Exception ex) {
            Log.warn(SettingsDialog.class, "Não foi possível guardar " + key, ex);
        }
    }
}
