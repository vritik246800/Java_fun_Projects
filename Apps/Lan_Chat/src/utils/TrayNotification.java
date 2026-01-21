package utils;

import java.awt.*;

public class TrayNotification {

    private static TrayIcon trayIcon;

    public static void init() {
        try {
            SystemTray tray = SystemTray.getSystemTray();

            Image image = Toolkit.getDefaultToolkit()
                    .getImage("icon.png"); // ícone simples

            trayIcon = new TrayIcon(image, "LAN Chat");
            trayIcon.setImageAutoSize(true);

            tray.add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void show(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(
                title,
                message,
                TrayIcon.MessageType.INFO
            );
        }
    }
}
