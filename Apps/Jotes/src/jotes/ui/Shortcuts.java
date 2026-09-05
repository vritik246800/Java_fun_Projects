package jotes.ui;

import jotes.db.SettingsRepository;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registo central de comandos e respetivos atalhos. Cada comando tem um id estável,
 * um rótulo, um atalho por omissão e uma ação; o atalho pode ser mudado pelo
 * utilizador e fica guardado em {@link SettingsRepository} sob {@code shortcut.<id>}.
 * <p>Serve as três frentes que precisam da mesma lista: os bindings de teclado
 * ({@link #install(JComponent)}), a paleta de comandos e a tela de ajuda de atalhos.</p>
 */
public class Shortcuts {

    /** Prefixo das chaves de definições onde os atalhos personalizados são guardados. */
    private static final String KEY_PREFIX = "shortcut.";

    /** Máscara de menu da plataforma (Ctrl no Windows/Linux, Cmd no macOS). */
    public static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    /** Um comando registado: identidade, rótulo, atalho e ação. */
    public static final class Command {
        private final String id;
        private final String label;
        private final String group;
        private final String icon;
        private final KeyStroke defaultStroke;
        private final Runnable action;
        private KeyStroke stroke;

        Command(String id, String label, String group, String icon,
                KeyStroke defaultStroke, Runnable action) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.icon = icon;
            this.defaultStroke = defaultStroke;
            this.stroke = defaultStroke;
            this.action = action;
        }

        public String id() { return id; }
        public String label() { return label; }
        public String group() { return group; }
        public String icon() { return icon; }
        public KeyStroke stroke() { return stroke; }
        public KeyStroke defaultStroke() { return defaultStroke; }
        public void run() { action.run(); }

        /** Atalho legível, ex. {@code "Ctrl+Shift+P"}; string vazia se não tiver atalho. */
        public String strokeText() {
            return format(stroke);
        }
    }

    private final SettingsRepository settings;
    private final Map<String, Command> commands = new LinkedHashMap<>();
    private JComponent installedOn;

    public Shortcuts(SettingsRepository settings) {
        this.settings = settings;
    }

    /**
     * Regista um comando. {@code defaultStroke} pode ser {@code null} para comandos
     * que só existem na paleta. O atalho guardado nas definições, se houver, ganha.
     */
    public Command register(String id, String label, String group, String icon,
                            KeyStroke defaultStroke, Runnable action) {
        Command c = new Command(id, label, group, icon, defaultStroke, action);
        KeyStroke saved = loadStroke(id);
        if (saved != null) c.stroke = saved;
        commands.put(id, c);
        return c;
    }

    /** Atalho com a máscara de menu da plataforma (Ctrl/Cmd) mais {@code keyCode}. */
    public static KeyStroke menu(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, MENU_MASK);
    }

    /** Atalho com a máscara de menu mais Shift. */
    public static KeyStroke menuShift(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, MENU_MASK | InputEvent.SHIFT_DOWN_MASK);
    }

    public List<Command> commands() {
        return new ArrayList<>(commands.values());
    }

    public Command get(String id) {
        return commands.get(id);
    }

    /**
     * Muda o atalho de um comando e persiste-o. Se outro comando já usava esse atalho,
     * fica sem atalho (não há duplicados). {@code stroke} a {@code null} remove o atalho.
     */
    public void setStroke(String id, KeyStroke stroke) {
        Command target = commands.get(id);
        if (target == null) return;
        if (stroke != null) {
            for (Command other : commands.values()) {
                if (other != target && stroke.equals(other.stroke)) {
                    other.stroke = null;
                    saveStroke(other.id, null);
                }
            }
        }
        target.stroke = stroke;
        saveStroke(id, stroke);
        if (installedOn != null) install(installedOn);
    }

    /** Repõe todos os atalhos por omissão. */
    public void resetAll() {
        for (Command c : commands.values()) {
            c.stroke = c.defaultStroke;
            saveStroke(c.id, null);
        }
        if (installedOn != null) install(installedOn);
    }

    /**
     * (Re)instala os bindings em {@code WHEN_IN_FOCUSED_WINDOW} do componente indicado.
     * Chamar depois de registar todos os comandos e a cada mudança de atalho.
     */
    public void install(JComponent root) {
        installedOn = root;
        InputMap in = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        in.clear(); // este mapa é exclusivo do painel raiz da janela
        for (Command c : commands.values()) {
            if (c.stroke == null) continue;
            in.put(c.stroke, c.id);
            am.put(c.id, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    c.run();
                }
            });
        }
    }

    // ---------------------------------------------------------------- persistência

    private KeyStroke loadStroke(String id) {
        try {
            String saved = settings.get(KEY_PREFIX + id, null);
            if (saved == null) return null;
            return saved.isBlank() ? null : KeyStroke.getKeyStroke(saved);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveStroke(String id, KeyStroke stroke) {
        try {
            settings.set(KEY_PREFIX + id, stroke == null ? "" : stroke.toString());
        } catch (Exception ignored) {
            // atalho continua ativo nesta sessão mesmo que não persista
        }
    }

    // ---------------------------------------------------------------- formatação

    /** Representação legível de um atalho, ex. {@code "Ctrl+Shift+P"}. */
    public static String format(KeyStroke stroke) {
        if (stroke == null) return "";
        StringBuilder sb = new StringBuilder();
        int mods = stroke.getModifiers();
        if ((mods & InputEvent.META_DOWN_MASK) != 0) sb.append("Cmd+");
        if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) sb.append("Ctrl+");
        if ((mods & InputEvent.ALT_DOWN_MASK) != 0) sb.append("Alt+");
        if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) sb.append("Shift+");
        String key = java.awt.event.KeyEvent.getKeyText(stroke.getKeyCode());
        return sb.append(key).toString();
    }
}
