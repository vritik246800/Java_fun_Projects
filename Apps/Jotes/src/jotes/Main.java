package jotes;

import jotes.db.Database;
import jotes.db.SettingsRepository;
import jotes.services.BackupService;
import jotes.ui.MainFrame;
import jotes.ui.SplashScreen;
import jotes.ui.Theme;
import jotes.ui.anim.Animations;
import jotes.util.Log;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Arranque da aplicação: mostra o splash, abre a base de dados e faz o backup de
 * arranque fora da thread da UI, e só depois constrói a janela principal.
 * <p>A pasta de dados pode ser dada como primeiro argumento; por omissão é
 * {@code ./data} junto ao diretório de trabalho.</p>
 */
public final class Main {

    /** Pasta de dados por omissão, relativa ao diretório de trabalho. */
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String DB_FILE = "jotes.db";

    private Main() {}

    public static void main(String[] args) {
        Path dataDir = Paths.get(args.length > 0 && !args[0].isBlank() ? args[0] : DEFAULT_DATA_DIR)
                .toAbsolutePath();
        Log.init(dataDir);
        Log.installGlobalHandler();
        Log.of(Main.class).info("A arrancar o Jotes em {}", dataDir);

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.application.appearance", "system");

        SwingUtilities.invokeLater(() -> {
            Theme.applyGlobal();
            SplashScreen splash = new SplashScreen();
            splash.showWithFade();
            loadInBackground(dataDir, splash);
        });
    }

    /**
     * Abre a base de dados e faz o backup de arranque numa thread de fundo (o SQLite
     * e a cópia dos anexos podem demorar), e constrói a janela na thread da UI.
     */
    private static void loadInBackground(Path dataDir, SplashScreen splash) {
        new SwingWorker<Database, Void>() {
            @Override
            protected Database doInBackground() throws Exception {
                Database db = new Database(dataDir.resolve(DB_FILE));
                try {
                    SettingsRepository settings = new SettingsRepository(db);
                    // preferências que têm de valer antes de existirem componentes
                    Animations.setEnabled(
                            !settings.getBoolean(SettingsRepository.UI_REDUCE_ANIMATIONS, false));
                    new BackupService(db).createStartupBackup();
                } catch (Exception ex) {
                    // um backup falhado não deve impedir a app de abrir
                    Log.warn(Main.class, "Backup de arranque falhou", ex);
                }
                return db;
            }

            @Override
            protected void done() {
                try {
                    Database db = get();
                    Theme.setDark(readDarkPreference(db));
                    MainFrame frame = new MainFrame(db);
                    splash.closeWithFade();
                    frame.showApp();
                } catch (Exception ex) {
                    splash.closeWithFade();
                    Log.error(Main.class, "Não foi possível arrancar", ex);
                    JOptionPane.showMessageDialog(null,
                            "Não foi possível abrir a base de dados:\n"
                                    + (ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage()),
                            "Jotes", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        }.execute();
    }

    /** Tema guardado (escuro por omissão), aplicado antes de a janela ser construída. */
    private static boolean readDarkPreference(Database db) {
        try {
            return new SettingsRepository(db).getBoolean(SettingsRepository.UI_DARK, true);
        } catch (Exception e) {
            return true;
        }
    }
}
