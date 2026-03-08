package com.mvvmcsv;

import com.mvvmcsv.view.MainWindow;
import javax.swing.*;

/**
 * Entrada da aplicação.
 * Lança a View no Event Dispatch Thread — regra obrigatória em Swing.
 */
public class Main {
    public static void main(String[] args) {
        // Look & Feel do sistema (opcional — remove se quiseres o L&F padrão)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
