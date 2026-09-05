# Jotes — Roadmap

Roadmap de evolução do Jotes: novas funcionalidades, melhorias de UI e animações.
Stack atual: Java 21 + Swing, SQLite, editor Markdown (RSyntaxTextArea), Canvas (JGraphX), grafo de notas.

``` prompt
faz todas as fazes de ROADMAP.md
quando finalizar uma fase marca ela [x]
```

---

## [x] Fase 1 — Fundação de Animações e Tema

Infraestrutura reutilizável antes de animar telas específicas.

- [x] `ui/anim/Animator.java` — motor de animação baseado em `javax.swing.Timer` (60 FPS, easing)
- [x] `ui/anim/Easing.java` — curvas: linear, ease-in, ease-out, ease-in-out, spring
- [x] `ui/anim/FadeTransition.java` — fade in/out genérico para `JComponent`
- [x] `ui/anim/SlideTransition.java` — transições de slide (painéis laterais, troca de nota)
- [x] Suporte a animação de cor (interpolação ARGB) para hovers e seleção
- [x] Respeitar configuração "reduzir animações" (acessibilidade) em `SettingsRepository`
- [x] Refatorar `Theme.java`: extrair paleta para `ThemeColors` (claro/escuro) com troca animada de tema
- [x] Toggle de tema claro/escuro na UI (hoje o tema é fixo)

## [x] Fase 2 — Animações na UI Existente

Aplicar a fundação da Fase 1 nos componentes atuais.

- [x] `NotesListPanel` — fade/slide ao trocar de nota selecionada; hover animado nos cards (`NoteCardRenderer`)
- [x] `SidebarPanel` — expandir/colapsar pastas com animação de altura
- [x] `EditorPanel` — fade suave ao carregar nota; indicador de "salvando…/salvo" animado
- [x] `PreviewPane` — crossfade ao alternar editar/preview
- [x] `BacklinksPanel` e `AttachmentsPanel` — fade-in ao receber conteúdo
- [x] `GraphWindow` — layout do grafo com animação de física (nós se acomodando suavemente)
- [x] `CanvasWindow`/`CanvasPanel` — zoom e pan suaves (interpolação, não saltos)
- [x] `HistoryDialog` — abertura com fade + slide
- [x] Scroll suave (smooth scrolling) nas listas e no editor
- [x] Splash screen de abertura com fade (app demora a carregar BD; dar feedback visual)

## [x] Fase 3 — Melhorias de UI/UX

- [x] Command palette (`Ctrl+K` / `Ctrl+P`) — buscar notas, comandos e tags num lugar só
- [x] Busca global com destaque de resultados e preview do trecho encontrado
- [x] Breadcrumbs do caminho da nota (pasta > subpasta > nota) no topo do editor
- [x] Barra de status: contagem de palavras/caracteres, tempo de leitura, última modificação
- [x] Tabs para múltiplas notas abertas simultaneamente
- [x] Modo foco / escrita sem distrações (esconde sidebar e lista)
- [x] Redimensionar sidebar e lista de notas arrastando (splitters com memória de posição)
- [x] Menu de contexto nas notas: renomear, duplicar, mover, excluir, fixar (pin)
- [x] Notas fixadas (pinned) no topo da lista
- [x] Ícones consistentes (substituir emojis/texto por ícone SVG ou fonte de ícones)
- [x] Empty states: telas amigáveis quando não há nota selecionada / lista vazia
- [x] Toasts de notificação (substituir `JOptionPane` em ações comuns: salvo, exportado, erro)
- [x] Atalhos de teclado configuráveis + tela de ajuda de atalhos (`Ctrl+/`)

## [x] Fase 4 — Editor e Markdown

- [x] Toolbar de formatação no editor (negrito, itálico, título, lista, link, código)
- [x] Autocompletar `[[` para wikilinks com busca fuzzy de notas
- [x] Autocompletar `#` para tags
- [x] Checklists interativas: clicar no `- [ ]` do preview marca no markdown
- [x] Tabelas editáveis no preview
- [x] Syntax highlight de blocos de código no preview (por linguagem)
- [x] Colar imagem da área de transferência direto na nota (salva em `data/attachments`)
- [x] Drag-and-drop de arquivos para anexar
- [x] Modo side-by-side editar+preview com scroll sincronizado
- [x] Templates de nota (diário, reunião, etc.) ao criar nova nota
- [x] Nota diária automática (atalho cria/abre a nota do dia)

## [x] Fase 5 — Novas Funcionalidades

- [x] Lixeira: exclusão suave com restauração (em vez de deletar direto)
- [x] Versionamento visual: diff entre versões no `HistoryDialog`
- [x] Exportação para PDF com estilo do tema (PDFBox já está no classpath)
- [x] Exportação de pasta inteira para HTML estático navegável
- [x] Importação de arquivos `.md` em massa (pasta → notas)
- [x] Alarmes/lembretes em notas (notificação no horário marcado)
- [x] Bloqueio de nota por senha (usar `CryptoService` existente na UI)
- [x] Backup automático agendado configurável (hoje `BackupService` existe; falta UI de config)
- [x] Estatísticas: notas criadas por semana, palavras totais, streak de escrita
- [x] Grafo filtrável (por pasta, tag, notas órfãs) com destaque de vizinhança ao clicar
- [x] Canvas: exportar board como imagem PNG/SVG
- [x] Sync: UI de configuração do `SyncService` (hoje sem interface aparente)

## [x] Fase 6 — Qualidade e Performance

- [x] Testes unitários dos repositórios e services (hoje não há testes)
- [x] Índice full-text search no SQLite (FTS5) para busca instantânea
- [x] Lazy loading da lista de notas (paginação/virtualização para milhares de notas)
- [x] Cache de render do preview Markdown (não re-renderizar a cada tecla)
- [x] Migrações de schema versionadas no `Database.java`
- [x] Empacotamento: `.exe`/instalador Windows (jpackage) com JRE embutido
- [x] Logging em arquivo (`data/logs/`) em vez de `printStackTrace`

---

## Bibliotecas externas sugeridas (evitar "raw code")

Bibliotecas prontas que facilitam os itens das fases acima. **Como adicionar:** clicar no link (mvnrepository.com), escolher a versão, baixar o `.jar` para `lib/<categoria>/` e adicionar uma linha `<classpathentry kind="lib" .../>` no `.classpath` (ou atualizar o projeto no Eclipse).

### Animações (Fases 1–2)

- [Trident](https://mvnrepository.com/artifact/org.pushingpixels/trident) — motor de animação Swing pronto (timelines, easing, interpolação de cor); substitui boa parte do `Animator`/`Easing` da Fase 1.

### UI e ícones (Fase 3)

- [MigLayout Swing](https://mvnrepository.com/artifact/com.miglayout/miglayout-swing) — layout manager potente para painéis complexos (toolbar, barra de status, command palette).
- [FlatLaf Extras](https://mvnrepository.com/artifact/com.formdev/flatlaf-extras) — `FlatSVGIcon` para ícones SVG que seguem o tema (substitui emojis/texto).
- [FlatLaf IntelliJ Themes](https://mvnrepository.com/artifact/com.formdev/flatlaf-intellij-themes) — dezenas de temas claro/escuro prontos para o toggle de tema.
- [jsvg](https://mvnrepository.com/artifact/com.github.weisj/jsvg) — renderizador SVG Java2D leve (ícones em alta resolução, exportar canvas para SVG).

### Editor e Markdown (Fase 4)

- [AutoComplete](https://mvnrepository.com/artifact/com.fifesoft/autocomplete) — autocompletar pronto para RSyntaxTextArea (usar para `[[wikilinks]]` e `#tags`).
- [jsoup](https://mvnrepository.com/artifact/org.jsoup/jsoup) — parse/manipulação de HTML para importação `.md`/HTML e tabelas editáveis no preview.

### Novas funcionalidades (Fase 5)

- [Quartz](https://mvnrepository.com/artifact/org.quartz-scheduler/quartz) — agendamento de tarefas (alarmes/lembretes e backup automático configurável).
- [java-diff-utils](https://mvnrepository.com/artifact/io.github.java-diff-utils/java-diff-utils) — diff entre textos para o versionamento visual no `HistoryDialog`.
- [Tika Parsers Standard](https://mvnrepository.com/artifact/org.apache.tika/tika-parsers-standard-package) — extrair texto de PDF/DOCX/etc. dos anexos para a busca global.

### Busca (Fases 3 e 6)

- [Apache Commons Text](https://mvnrepository.com/artifact/org.apache.commons/commons-text) — fuzzy search (Levenshtein, Jaro-Winkler) para a command palette e busca global.

### Qualidade (Fase 6)

- [SLF4J API](https://mvnrepository.com/artifact/org.slf4j/slf4j-api) + [Logback Classic](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic) — logging em arquivo (`data/logs/`) em vez de `printStackTrace`.
- [JUnit Jupiter](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter) — testes unitários dos repositórios e services.

### Atenção

- `lib/pdf/pdfbox-3.0.7-javadoc.jar` e `lib/extract/tika-core-3.3.1-javadoc.jar` são JARs de **javadoc** (sem classes executáveis); os JARs principais já estão no `lib/`.
- O PDFBox 3 precisa ainda de `pdfbox-io` e `commons-logging`, ambos já adicionados (`lib/pdf/pdfbox-io-3.0.7.jar` e `lib/log/commons-logging-1.3.5.jar`). Sem eles a exportação para PDF rebenta com `NoClassDefFoundError`.

---

### Convenções deste roadmap

- Cada item deve ser implementado com mudança mínima e verificada antes de marcar `[x]`.
- Fases são sequenciais em prioridade, mas itens dentro de uma fase podem ser reordenados.
- Animações (Fases 1–2) nunca devem bloquear a thread da UI nem degradar em máquinas fracas — sempre com flag para desativar.

---

## Estado

Todas as fases concluídas. Como verificar:

```bash
bash build.sh          # compila src/ + test/ para bin/
bash package.sh        # instalador nativo com JRE embutido (jpackage)
java -cp "bin:$(ls lib/*/*.jar | tr '\n' ':')" jotes.Main [pasta-de-dados]
```

Testes (JUnit 5, sem framework de build):

```bash
CP="bin:$(ls lib/*/*.jar | tr '\n' ':')"
java -jar lib/test/junit-platform-console-standalone-1.12.2.jar \
     execute --class-path "$CP" --scan-class-path bin
```
