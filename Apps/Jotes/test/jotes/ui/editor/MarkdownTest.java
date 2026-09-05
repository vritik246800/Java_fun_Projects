package jotes.ui.editor;

import jotes.services.HtmlExportService;
import jotes.util.LinkParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da manipulação de markdown: wikilinks, tarefas, tabelas e modelos. */
class MarkdownTest {

    @Test
    @DisplayName("extrai os títulos dos wikilinks, com e sem alias")
    void extractsWikilinkTitles() {
        var titles = LinkParser.extractTitles("ver [[Uma nota]] e [[Outra|com alias]] e [[Uma nota]]");
        assertEquals(2, titles.size());
        assertTrue(titles.contains("Uma nota"));
        assertTrue(titles.contains("Outra"));
    }

    @Test
    @DisplayName("os wikilinks viram links jotes: na pré-visualização")
    void wikilinksBecomeInternalLinks() {
        String html = PreviewPane.preprocessLinks("ver [[Uma nota|texto]]");
        assertEquals("ver [texto](jotes:Uma%20nota)", html);
    }

    @Test
    @DisplayName("as caixas de tarefa são numeradas por ordem, ignorando blocos de código")
    void taskLinesSkipCodeBlocks() {
        String markdown = """
                - [ ] primeira
                texto
                - [x] segunda
                ```
                - [ ] isto é código
                ```
                - [ ] terceira""";
        assertEquals(List.of(0, 2, 6), PreviewPane.taskLines(markdown));
    }

    @Test
    @DisplayName("marcar uma tarefa inverte só a caixa pedida")
    void toggleTaskFlipsOnlyThatBox() {
        String markdown = "- [ ] a\n- [ ] b";
        assertEquals("- [x] a\n- [ ] b", PreviewPane.toggleTask(markdown, 0));
        assertEquals("- [ ] a\n- [x] b", PreviewPane.toggleTask(markdown, 1));
        assertEquals(markdown, PreviewPane.toggleTask(markdown, 9));
    }

    @Test
    @DisplayName("desmarcar uma tarefa já marcada volta a [ ]")
    void toggleTaskUnchecks() {
        assertEquals("- [ ] feito", PreviewPane.toggleTask("- [x] feito", 0));
    }

    @Test
    @DisplayName("localiza uma tabela markdown e devolve as suas células")
    void findsMarkdownTable() {
        String markdown = """
                antes

                | Nome | Idade |
                | --- | ---: |
                | Ana | 30 |
                | Rui | 41 |

                depois""";
        TableEditorDialog.Located table = TableEditorDialog.find(markdown, 0);
        assertNotNull(table);
        assertEquals(3, table.rows().size());
        assertEquals(List.of("Nome", "Idade"), table.rows().get(0));
        assertEquals(List.of("Ana", "30"), table.rows().get(1));
        assertNull(TableEditorDialog.find(markdown, 1));
    }

    @Test
    @DisplayName("substituir a tabela mantém o texto à volta")
    void replaceTableKeepsSurroundingText() {
        String markdown = "antes\n| a |\n| --- |\n| 1 |\ndepois";
        TableEditorDialog.Located table = TableEditorDialog.find(markdown, 0);
        String result = TableEditorDialog.replace(markdown, table, "| novo |\n| --- |\n| 2 |");
        assertTrue(result.startsWith("antes\n"));
        assertTrue(result.endsWith("\ndepois"));
        assertTrue(result.contains("| novo |"));
    }

    @Test
    @DisplayName("os modelos resolvem os marcadores de data")
    void templatesRenderPlaceholders() {
        String rendered = Templates.render("hoje é {{data}} ({{diaDaSemana}})");
        assertTrue(rendered.matches("hoje é \\d{2}/\\d{2}/\\d{4} \\([\\p{L}-]+\\)"), rendered);
        assertTrue(Templates.dailyTitle().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals("Diário", Templates.daily().name());
    }

    @Test
    @DisplayName("o slug do exportador HTML é seguro para nome de ficheiro")
    void htmlSlugIsFileSafe() {
        assertEquals("acao-reacao", HtmlExportService.slug("Ação & Reação"));
        assertEquals("nota", HtmlExportService.slug("///"));
    }
}
