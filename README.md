# Java_Facul_Backup

Backup dos meus projetos Java de faculdade — exercícios, trabalhos práticos e aplicações completas desenvolvidas ao longo da Licenciatura em Engenharia Informática.

> **Nota**: repositório de arquivo pessoal. Cada pasta é um projeto Eclipse/VS Code independente, sem build system partilhado.

![Java](https://img.shields.io/badge/Java-22-orange)
![Swing](https://img.shields.io/badge/GUI-Swing%20%2F%20AWT-blue)
![Projetos](https://img.shields.io/badge/projetos-80%2B-green)

---

## 📊 Números

| | |
|---|---|
| Projetos | ~84 (80 em `Apps/`, 4 em `Componentes/`) |
| Ficheiros `.java` | 644 |
| Linhas de código | ~48 700 |
| JDK dominante | Java 22 (também 17, 21, 24, 25) |
| GUI | Swing / AWT puro (1 teste em JavaFX) |
| Build | `javac` + `jar` manual (1 projeto Maven) |

---

## 📋 Índice

- [Estrutura do Repositório](#-estrutura-do-repositório)
- [Aplicações em Destaque](#-aplicações-em-destaque)
- [Exercícios por Disciplina](#-exercícios-por-disciplina)
- [Componentes Reutilizáveis](#-componentes-reutilizáveis)
- [Pré-requisitos](#-pré-requisitos)
- [Como Compilar e Executar](#-como-compilar-e-executar)
- [Bibliotecas Externas](#-bibliotecas-externas)
- [Documentação](#-documentação)
- [Autor](#-autor)

---

## 📁 Estrutura do Repositório

```
Java_Facul_Backup/
├── Apps/                  # 80 projetos: aplicações, exercícios e trabalhos
├── Componentes/           # Componentes Swing personalizados e utilitários
├── JavaFX_Teste/          # Único projeto JavaFX (src/ + lib/)
├── EDA2_Excercios_Pratico_1/
├── Aresta/  Edge/  OpenWEBJava/   # legado (apenas bin/, sem fontes)
├── GUIA_Java_UI.md        # Guia completo de Java Swing (~900 linhas)
├── Para_ter_JAR_EXEC.md   # Como gerar JAR executável
├── 100_java_projects.md   # Checklist de 100 ideias de projetos
└── README.md
```

Estrutura típica de cada projeto (Eclipse):

```
Apps/NomeDoProjeto/
├── src/          # Código-fonte .java
├── bin/          # .class compilados
├── .classpath    # Config Eclipse (JDK + libs)
└── .project
```

---

## 🚀 Aplicações em Destaque

### Jogos
| Projeto | Descrição |
|---|---|
| `Apps/SnakeGame` | Jogo da cobra em duas versões (`SnakeMega`, `SnakeGameUltraOptimized`), com highscore e gravação de replays |
| `Apps/Xadrez` | Tabuleiro de xadrez com regras de movimento das peças |
| `Apps/Gato` | Desenho de um gato em ASCII art com ciclos aninhados |

### Rede (LAN)
| Projeto | Descrição |
|---|---|
| `Apps/Lan_Chat` | Chat via sockets TCP na rede local |
| `Apps/File_Transfer` | Transferência de ficheiros entre máquinas (JAR incluído) |
| `Apps/LAN_CHAT_FILE` | Merge do chat + transferência de ficheiros num só cliente |
| `Apps/Remote_Access` | Captura e envio do ecrã de outra máquina na rede local |

### Gestão e Faturação
| Projeto | Descrição |
|---|---|
| `Apps/SuperMercado` | Gestão de supermercado (com scripts SQL em `SQL/`) |
| `Apps/Sistema_Gestao_Loja` | Gestão de loja |
| `Apps/FacturaPOO` / `Apps/FaturaMVL` | Emissão de faturas |
| `Apps/Calculadora_Loja` | Cálculo de preço base e preço final |
| `Apps/Uber` | Simulação de sistema de viagens |

### Grafos
| Projeto | Descrição |
|---|---|
| `Apps/TAD-Grafo-master` | TAD Grafo — implementação da estrutura |
| `Apps/Graph_Note` / `Apps/Note_Grafo` | Notas ligadas em grafo, com visualização |
| `Apps/Grafo_Projectar_Mapeamento` | Projeção e mapeamento de grafos (V1 incluída) |

### Produtividade e Dados
| Projeto | Descrição |
|---|---|
| `Apps/NotePad` | Editor de texto com persistência (`Data/`, `Image/`, JAR incluído) |
| `Apps/Teste_GUI_CSV_PesquisaDinamica_Idea_BDBarato` | Leitura/escrita de CSV com pesquisa dinâmica em Swing + Graphics2D |
| `Apps/Teste_mysql`, `Apps/Ligacao_BASE_DADOS_SQL_TESTE` | Ligação JDBC a MySQL |
| `Apps/TesteJava_SQLPLUS` | Ligação a Oracle SQL*Plus |

### Interfaces Gráficas
`Apps/FlyWell_GUI` · `Apps/Hollard_Seguros_GUI` · `Apps/Recargs_GUI` · `Apps/PaginaLogin` · `Apps/Teste_de_JTree`

---

## 📚 Exercícios por Disciplina

### EDA — Estruturas de Dados e Algoritmos
Implementações de raiz, sem usar as coleções da biblioteca padrão:

- **Estruturas base**: `EDA_Fila`, `EDA_Pilha`, `EDA_Lista`, `EDA_List_V2`, `EDA_List_V3`
- **Casos práticos**: `EDA_BancoNOVO`, `EDA_Esquadra` (V1 → V3), `EDA_EsquadraVritik`
- **Prático**: `EDA2_Excercios_Pratico_1`

### POO — Programação Orientada a Objetos
- **Herança e polimorfismo**: `Heranca1`, `Heranca2`, `Conta_Heranca`, `Obra_Heranca`, `Oficial_Heranca`, `CanalHeranca`, `Bilheite_Heranca`, `ProGymHeranca`
- **Coleções e arrays**: `ArrayList`, `Array_de_Objecto`, `JogadorAL`, `RecordanciaPOO2ArrayList`
- **Exames e recorrências**: `Teste2024_Correcao_POO2`, `Teste2_poo2_2023`, `RecordanciaPOO1`, `RecordanciaPOO11`, `VisitaExameRecorenciaPOO2ArrayList`, `CigarroExameRecorenciaPOO2ArrayList`
- **Trabalhos práticos**: `Trabalho_Pratico_1_POO2`, `Trabalho_Pratico_1_V1_POO2`, `TrabalhoPratico2`
- **Exercícios numerados**: `Exercicio_36` a `Excercicio_40`, `Trabalho_36`, `Excercicio_Bebidas`

---

## 🧩 Componentes Reutilizáveis

Em `Componentes/` — código partilhado entre projetos:

| Ficheiro | Função |
|---|---|
| `BotaoModerno.java` | Botão Swing personalizado (~25 KB, o componente maior) |
| `AnimadorComboio.java` | Animação de comboio sobre Graphics2D |
| `AnimadorGrafo.java` | Animação de grafos |
| `EfeitoExplosao.java` | Efeito visual de explosão |
| `RelatorioCSV.java` | Geração de relatórios em CSV |
| `RelatorioPDF.java` | Geração de relatórios em PDF (iText/OpenPDF) |

Subprojetos: `A_componentes/`, `Interface_Grafica_GUI/`, `JDate_Teste/` (date picker), `Switch_teste/` (toggle switch).

---

## 🔧 Pré-requisitos

- **JDK 22** ou superior (a maioria dos projetos aponta para `JavaSE-22` no `.classpath`; alguns usam 17, 21, 24 ou 25)
- **IDE**: Eclipse (formato nativo dos projetos) ou VS Code com Extension Pack for Java
- **MySQL** — só para os projetos com JDBC

Verificar instalação:

```bash
java -version
javac -version
```

---

## ▶️ Como Compilar e Executar

Não há build system central. Cada projeto compila com `javac`:

```bash
cd Apps/SnakeGame
javac -d bin src/*.java
java -cp bin NomeDaClassePrincipal
```

Com bibliotecas externas (`lib/`):

```bash
javac -cp "lib/*" -d bin src/*.java
java -cp "bin:lib/*" NomeDaClassePrincipal     # Linux/macOS
java -cp "bin;lib/*" NomeDaClassePrincipal     # Windows
```

### Gerar JAR executável

```bash
javac -d bin src/*.java

# criar MANIFEST.MF com:
#   Manifest-Version: 1.0
#   Main-Class: NomeDaClassePrincipal
#   (linha em branco no fim — obrigatória)

jar cfm NomeDoJar.jar MANIFEST.MF -C bin .
java -jar NomeDoJar.jar
```

Detalhes completos em [`Para_ter_JAR_EXEC.md`](Para_ter_JAR_EXEC.md).

### Importar no Eclipse

`File` → `Import` → `Existing Projects into Workspace` → escolher a pasta do projeto.

---

## 📦 Bibliotecas Externas

JARs incluídos nos projetos que os usam (sem gestor de dependências):

| Biblioteca | Versão | Uso |
|---|---|---|
| `mysql-connector-j` | 8.4.0 | Ligação JDBC a MySQL |
| `gson` | 2.10.1 | Serialização JSON |
| `jfreechart` + `jcommon` | 1.5.4 / 1.0.24 | Gráficos e charts |
| `itextpdf` | 5.5.13 | Geração de PDF |
| `openpdf` | 1.3.30 | Geração de PDF (alternativa open-source) |

---

## 📖 Documentação

| Ficheiro | Conteúdo |
|---|---|
| [`GUIA_Java_UI.md`](GUIA_Java_UI.md) | Guia completo de Java Swing: layouts, menus, componentes, eventos (~900 linhas) |
| [`Para_ter_JAR_EXEC.md`](Para_ter_JAR_EXEC.md) | Passo a passo para gerar e executar JARs |
| [`100_java_projects.md`](100_java_projects.md) | Checklist de 100 ideias de projetos, com progresso marcado |

---

## 📝 Convenção de Commits

```
feat:     nova funcionalidade
fix:      correção de bug
docs:     documentação
refactor: refatoração
chore:    manutenção
```

---

## 👤 Autor

**Vritik Valabdas**
Licenciatura em Engenharia Informática

- GitHub: [@vritik246800](https://github.com/vritik246800)
- Repositório: [Java_Facul_Backup](https://github.com/vritik246800/Java_Facul_Backup)

---

*Repositório de arquivo académico — os projetos refletem a evolução da aprendizagem, não código de produção.*
