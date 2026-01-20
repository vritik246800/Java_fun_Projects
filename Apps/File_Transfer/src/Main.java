import discovery.UdpDiscovery;
import discovery.UdpListener;
import network.FileReceiver;
import ui.MainWindow;

import javax.swing.SwingUtilities;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.net.URL;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // ===== ÍCONE DO DOCK (macOS) =====
            try {
                URL iconURL = Main.class.getResource("/image/icon.png");
                if (iconURL == null) {
                    throw new RuntimeException("Ícone do Dock não encontrado !");
                }

                Image dockIcon = Toolkit.getDefaultToolkit().getImage(iconURL);
                Taskbar taskbar = Taskbar.getTaskbar();

                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(dockIcon);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // =================================

            // ===== UI =====
            MainWindow ui = new MainWindow();
            ui.setVisible(true);

            // ===== NETWORK / THREADS =====
            FileReceiver receiver = new FileReceiver(ui);
            ui.setFileReceiver(receiver);

            new Thread(receiver).start();
            new Thread(new UdpListener()).start();
            new Thread(new UdpDiscovery(ui::addDevice)).start();
        });
    }
}
