package jotes.ui.editor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Modelos de nota aplicados ao criar uma nota nova. Cada modelo devolve título e
 * conteúdo já com a data resolvida — os marcadores {@code {{data}}},
 * {@code {{hora}}} e {@code {{diaDaSemana}}} são substituídos em
 * {@link #render(String)}.
 */
public final class Templates {

    private static final Locale PT = Locale.of("pt");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /** Título da nota diária: também é a chave usada para a reabrir. */
    public static final DateTimeFormatter DAILY_TITLE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Um modelo: nome no menu, título sugerido e corpo em markdown. */
    public record Template(String name, String title, String body) {}

    private Templates() {}

    /** Modelos disponíveis, na ordem em que aparecem no menu. */
    public static List<Template> all() {
        return List.of(
                new Template("Nota em branco", "", ""),
                new Template("Diário", "{{data}}", """
                        # {{data}} · {{diaDaSemana}}

                        ## Como correu

                        ## O que fiz
                        - [ ]\s

                        ## Para amanhã
                        - [ ]\s
                        """),
                new Template("Reunião", "Reunião — {{data}}", """
                        # Reunião — {{data}} {{hora}}

                        **Presentes:**\s
                        **Assunto:**\s

                        ## Notas
                        -\s

                        ## Decisões
                        -\s

                        ## Ações
                        - [ ] (responsável) —\s
                        """),
                new Template("Tarefas", "Tarefas — {{data}}", """
                        # Tarefas

                        ## Hoje
                        - [ ]\s

                        ## Esta semana
                        - [ ]\s

                        ## Um dia
                        - [ ]\s
                        """),
                new Template("Ideia", "Ideia — ", """
                        # Ideia

                        ## O problema

                        ## A ideia

                        ## Próximo passo
                        - [ ]\s
                        """),
                new Template("Leitura", "Leitura — ", """
                        # Leitura

                        **Autor:**\s
                        **Fonte:**\s

                        ## Resumo

                        ## Citações
                        >\s

                        ## O que retenho
                        -\s
                        """));
    }

    /** O modelo com este nome, ou o primeiro da lista se não existir. */
    public static Template byName(String name) {
        for (Template t : all()) {
            if (t.name().equals(name)) return t;
        }
        return all().get(0);
    }

    /** O modelo usado pela nota diária. */
    public static Template daily() {
        return byName("Diário");
    }

    /** Título da nota diária de hoje (também serve de chave para a reabrir). */
    public static String dailyTitle() {
        return DAILY_TITLE.format(LocalDate.now());
    }

    /** Substitui os marcadores de data e hora pelo momento atual. */
    public static String render(String text) {
        if (text == null) return "";
        LocalDateTime now = LocalDateTime.now();
        String weekday = now.getDayOfWeek().getDisplayName(TextStyle.FULL, PT);
        return text.replace("{{data}}", DATE.format(now))
                .replace("{{hora}}", TIME.format(now))
                .replace("{{diaDaSemana}}", weekday);
    }
}
