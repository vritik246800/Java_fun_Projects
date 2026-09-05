package jotes.ui;

import jotes.ui.anim.FadeTransition;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.Duration;

/**
 * Ecrã de arranque: janela pequena sem decoração, centrada no ecrã, com o nome
 * da aplicação, visível enquanto a bd e a janela principal são carregadas.
 */
public class SplashScreen extends JWindow {

    /** Duração dos fades de entrada e saída, em ms. */
    private static final int FADE_MS = 200;

    public SplashScreen() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        // contorno fino, já que a janela não tem decoração própria
        content.setBorder(BorderFactory.createLineBorder(Theme.SEPARATOR));

        JLabel name = new JLabel("Jotes", SwingConstants.CENTER);
        name.setForeground(Theme.TEXT);
        name.setFont(Theme.font(Font.BOLD, 28));
        content.add(name, BorderLayout.CENTER);

        JLabel loading = new JLabel("A carregar…", SwingConstants.CENTER);
        loading.setForeground(Theme.TEXT_DIM);
        loading.setFont(Theme.font(Font.PLAIN, 12));
        loading.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        content.add(loading, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(320, 200);
        setLocationRelativeTo(null);
    }

    /** Mostra o splash com um fade de entrada (não bloqueia). */
    public void showWithFade() {
        FadeTransition.fadeIn(this, Duration.ofMillis(FADE_MS));
    }

    /** Esbate a janela e destrói-a no fim da animação. */
    public void closeWithFade() {
        FadeTransition.fadeOut(this, Duration.ofMillis(FADE_MS), this::dispose);
    }
}
