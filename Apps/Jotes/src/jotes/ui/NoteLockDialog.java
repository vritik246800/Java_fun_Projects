package jotes.ui;

import jotes.db.NoteRepository;
import jotes.model.Note;
import jotes.services.CryptoService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Bloqueio e desbloqueio de uma nota com palavra-passe, usando o
 * {@link CryptoService} (AES-GCM com chave derivada por PBKDF2). O conteúdo
 * cifrado fica guardado em base64 no próprio campo {@code content} e a nota
 * é marcada com {@code is_locked}.
 * <p>A palavra-passe não é guardada em lado nenhum: perdê-la é perder a nota.</p>
 */
public final class NoteLockDialog {

    /** Prefixo que marca um conteúdo cifrado, para nunca ser mostrado como texto. */
    public static final String PREFIX = "jotes-locked:v1:";

    private NoteLockDialog() {}

    /**
     * Pede uma palavra-passe (duas vezes) e cifra o conteúdo da nota.
     * Devolve {@code true} se a nota ficou bloqueada.
     */
    public static boolean lock(Window owner, Note note, NoteRepository noteRepo, CryptoService crypto) {
        char[] password = ask(owner, "Bloquear nota",
                "Escolhe uma palavra-passe para «" + note.displayTitle() + "».", true);
        if (password == null) return false;
        try {
            byte[] cipher = crypto.encrypt(note.getContent().getBytes(StandardCharsets.UTF_8), password);
            note.setContent(PREFIX + Base64.getEncoder().encodeToString(cipher));
            note.setLocked(true);
            noteRepo.save(note);
            noteRepo.setLocked(note.getId(), true);
            return true;
        } catch (Exception ex) {
            error(owner, "Não foi possível cifrar a nota: " + ex.getMessage());
            return false;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Pede a palavra-passe e decifra o conteúdo da nota, deixando-a desbloqueada.
     * Devolve {@code true} em caso de sucesso.
     */
    public static boolean unlock(Window owner, Note note, NoteRepository noteRepo, CryptoService crypto) {
        char[] password = ask(owner, "Desbloquear nota",
                "Palavra-passe de «" + note.displayTitle() + "».", false);
        if (password == null) return false;
        try {
            String payload = note.getContent().startsWith(PREFIX)
                    ? note.getContent().substring(PREFIX.length()) : note.getContent();
            byte[] plain = crypto.decrypt(Base64.getDecoder().decode(payload.trim()), password);
            note.setContent(new String(plain, StandardCharsets.UTF_8));
            note.setLocked(false);
            noteRepo.save(note);
            noteRepo.setLocked(note.getId(), false);
            return true;
        } catch (Exception ex) {
            // qualquer falha aqui é, na prática, palavra-passe errada
            error(owner, "Palavra-passe errada ou nota corrompida.");
            return false;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /** {@code true} se o conteúdo estiver cifrado (pelo prefixo ou pela flag da nota). */
    public static boolean isLocked(Note note) {
        return note != null && (note.isLocked() || note.getContent().startsWith(PREFIX));
    }

    // ---------------------------------------------------------------- diálogo

    /** Pede a palavra-passe; com {@code confirm} pede-a duas vezes e valida a repetição. */
    private static char[] ask(Window owner, String title, String message, boolean confirm) {
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        JPasswordField first = new JPasswordField(20);
        JPasswordField second = new JPasswordField(20);
        JLabel warning = new JLabel(" ");
        char[][] result = new char[1][];

        for (JPasswordField field : new JPasswordField[]{first, second}) {
            Theme.styleTextField(field);
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            field.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        JLabel prompt = new JLabel("<html>" + message + "</html>");
        prompt.setForeground(Theme.TEXT);
        prompt.setFont(Theme.font(Font.PLAIN, 13));
        prompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        warning.setForeground(Theme.DANGER);
        warning.setFont(Theme.font(Font.PLAIN, 11));
        warning.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(18, 18, 8, 18));
        body.add(prompt);
        body.add(Box.createVerticalStrut(12));
        body.add(first);
        if (confirm) {
            body.add(Box.createVerticalStrut(8));
            body.add(second);
            JLabel hint = new JLabel("Sem a palavra-passe a nota não pode ser recuperada.");
            hint.setForeground(Theme.TEXT_DIM);
            hint.setFont(Theme.font(Font.PLAIN, 11));
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(Box.createVerticalStrut(8));
            body.add(hint);
        }
        body.add(Box.createVerticalStrut(6));
        body.add(warning);

        JButton ok = new JButton(confirm ? "Bloquear" : "Desbloquear");
        Theme.stylePrimaryButton(ok);
        JButton cancel = new JButton("Cancelar");
        Theme.styleButton(cancel);
        cancel.addActionListener(e -> dialog.dispose());

        Runnable accept = () -> {
            char[] a = first.getPassword();
            if (a.length < 4) {
                warning.setText("Usa pelo menos 4 caracteres.");
                return;
            }
            if (confirm && !Arrays.equals(a, second.getPassword())) {
                warning.setText("As palavras-passe não coincidem.");
                return;
            }
            result[0] = a;
            dialog.dispose();
        };
        ok.addActionListener(e -> accept.run());
        first.addActionListener(e -> {
            if (confirm) second.requestFocusInWindow();
            else accept.run();
        });
        second.addActionListener(e -> accept.run());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.add(body, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JPanel.WHEN_IN_FOCUSED_WINDOW);
        dialog.setSize(420, confirm ? 300 : 230);
        dialog.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(first::requestFocusInWindow);
        dialog.setVisible(true);
        return result[0];
    }

    private static void error(Window owner, String message) {
        javax.swing.JOptionPane.showMessageDialog(owner, message, "Bloqueio de nota",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }
}
