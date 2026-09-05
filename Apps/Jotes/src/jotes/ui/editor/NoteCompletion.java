package jotes.ui.editor;

import jotes.db.NoteRepository;
import jotes.db.TagRepository;
import jotes.model.Note;
import jotes.util.Log;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProviderBase;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Autocompletar do editor sobre a biblioteca AutoComplete do RSyntaxTextArea:
 * {@code [[} propõe títulos de notas e {@code #} propõe tags existentes, ambos
 * com correspondência difusa (subcadeia e subsequência).
 * <p>A popup abre-se sozinha assim que se escreve {@code [[} ou {@code #}, e
 * também com Ctrl+Espaço em qualquer altura.</p>
 */
public final class NoteCompletion {

    private NoteCompletion() {}

    /** Instala o autocompletar no editor indicado. */
    public static void install(RSyntaxTextArea area, NoteRepository noteRepo, TagRepository tagRepo) {
        Provider provider = new Provider(noteRepo, tagRepo);
        AutoCompletion completion = new AutoCompletion(provider);
        completion.setAutoActivationEnabled(false); // o disparo é nosso, por prefixo
        completion.setAutoCompleteSingleChoices(false);
        completion.setShowDescWindow(false);
        completion.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK));
        completion.install(area);

        // abre a popup logo a seguir a "[[" ou a "#", sem esperar por Ctrl+Espaço
        area.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (e.getLength() != 1) return;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    String prefix = provider.getAlreadyEnteredText(area);
                    if (prefix != null && !prefix.isEmpty()) completion.doCompletion();
                });
            }

            public void removeUpdate(DocumentEvent e) {}
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    /**
     * Fornece as sugestões. O "texto já escrito" inclui o marcador ({@code [[} ou
     * {@code #}) para o AutoComplete o substituir por inteiro ao aceitar.
     */
    private static final class Provider extends CompletionProviderBase {

        private final NoteRepository noteRepo;
        private final TagRepository tagRepo;

        Provider(NoteRepository noteRepo, TagRepository tagRepo) {
            this.noteRepo = noteRepo;
            this.tagRepo = tagRepo;
            setListCellRenderer(null);
        }

        /** Devolve o marcador + o que já foi escrito, ou "" se o cursor não está num. */
        @Override
        public String getAlreadyEnteredText(JTextComponent component) {
            int caret = component.getCaretPosition();
            Document doc = component.getDocument();
            try {
                int lineStart = lineStart(component, caret);
                String before = doc.getText(lineStart, caret - lineStart);

                int wiki = before.lastIndexOf("[[");
                if (wiki >= 0 && before.indexOf("]]", wiki) < 0) {
                    return before.substring(wiki);
                }
                int hash = before.lastIndexOf('#');
                if (hash >= 0) {
                    String typed = before.substring(hash + 1);
                    // uma tag não tem espaços e '#' no início da linha é um título markdown
                    boolean heading = before.substring(0, hash).isBlank();
                    if (!heading && !typed.contains(" ")) return before.substring(hash);
                }
            } catch (BadLocationException ex) {
                Log.warn(NoteCompletion.class, "Falha a ler o texto para autocompletar", ex);
            }
            return "";
        }

        private int lineStart(JTextComponent component, int caret) throws BadLocationException {
            String all = component.getDocument().getText(0, caret);
            int nl = all.lastIndexOf('\n');
            return nl + 1;
        }

        @Override
        public List<Completion> getCompletionsAt(JTextComponent component, java.awt.Point point) {
            return List.of();
        }

        @Override
        public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent component) {
            return null;
        }

        @Override
        protected List<Completion> getCompletionsImpl(JTextComponent component) {
            String entered = getAlreadyEnteredText(component);
            List<Completion> out = new ArrayList<>();
            if (entered.startsWith("[[")) {
                String term = entered.substring(2).toLowerCase(Locale.ROOT);
                try {
                    for (Note n : noteRepo.list(NoteRepository.Query.all().withPage(200, 0))) {
                        String title = n.displayTitle();
                        if (!matches(title, term)) continue;
                        out.add(new BasicCompletion(this, "[[" + title + "]]", title));
                    }
                } catch (Exception ex) {
                    Log.warn(NoteCompletion.class, "Falha a listar notas para autocompletar", ex);
                }
            } else if (entered.startsWith("#")) {
                String term = entered.substring(1).toLowerCase(Locale.ROOT);
                try {
                    for (String tag : tagRepo.listAll()) {
                        if (!matches(tag, term)) continue;
                        out.add(new BasicCompletion(this, "#" + tag, "tag"));
                    }
                } catch (Exception ex) {
                    Log.warn(NoteCompletion.class, "Falha a listar tags para autocompletar", ex);
                }
            }
            return out;
        }

        /** Correspondência difusa: subcadeia ou letras por ordem. */
        static boolean matches(String candidate, String term) {
            if (term.isEmpty()) return true;
            String c = candidate.toLowerCase(Locale.ROOT);
            if (c.contains(term)) return true;
            int i = 0;
            for (int j = 0; j < c.length() && i < term.length(); j++) {
                if (c.charAt(j) == term.charAt(i)) i++;
            }
            return i == term.length();
        }
    }
}
