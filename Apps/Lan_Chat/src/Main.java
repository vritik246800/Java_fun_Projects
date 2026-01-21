import java.awt.*;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ui.MainWindow;

public class Main {

    static {
        try {
            URL iconURL = Main.class.getResource("/image/icon.png");

            if (iconURL == null) {
                throw new RuntimeException("Ícone do Dock não encontrado em /image/icon.png");
            }

            Image dockIcon = Toolkit.getDefaultToolkit().getImage(iconURL);
            Taskbar taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(dockIcon);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Nome de utilizador:");
        if (name == null || name.isBlank()) return;

        SwingUtilities.invokeLater(() ->
                new MainWindow(name).setVisible(true)
        );
        
    }
}
