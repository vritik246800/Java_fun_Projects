# MVVM CSV Manager — Swing + AWT + Graphics2D

## Estrutura do projeto

```
src/
└── com/mvvmcsv/
    ├── Main.java                        ← ponto de entrada
    ├── model/
    │   └── Person.java                  ← MODEL: dados puros
    ├── viewmodel/
    │   └── PersonViewModel.java         ← VIEWMODEL: lógica + estado
    ├── view/
    │   ├── MainWindow.java              ← VIEW: UI Swing
    │   └── UIComponents.java            ← componentes G2D customizados
    └── util/
        └── CsvUtil.java                 ← leitura/gravação CSV
```

---

## Arquitetura MVVM

```mermaid
graph TD
    V["🖥️ View\nMainWindow.java\nUIComponents.java"]
    VM["⚙️ ViewModel\nPersonViewModel.java"]
    M["📦 Model\nPerson.java"]
    U["🗂️ Util\nCsvUtil.java"]
    CSV[("📄 pessoas.csv")]

    V -->|"chama addPerson / removePerson\nsaveToCSV / loadFromCSV / search"| VM
    VM -->|"callbacks: onDataChanged\nonSearchResult / onMessage"| V
    VM -->|"cria e lê"| M
    VM -->|"delega I/O"| U
    U -->|"lê / grava"| CSV

    style V   fill:#1a1a2e,stroke:#63b3ed,color:#e2e8f0
    style VM  fill:#1a1a2e,stroke:#9a75ea,color:#e2e8f0
    style M   fill:#1a1a2e,stroke:#48c78e,color:#e2e8f0
    style U   fill:#1a1a2e,stroke:#f0524a,color:#e2e8f0
    style CSV fill:#1a1a2e,stroke:#718096,color:#e2e8f0
```

**Regras rígidas:**
- A View **nunca** acede ao Model diretamente
- O ViewModel **nunca** importa `javax.swing` ou `java.awt`
- O CsvUtil não conhece nem a View nem o ViewModel

---

## Fluxo — Adicionar pessoa

```mermaid
sequenceDiagram
    actor U as Utilizador
    participant V as View (MainWindow)
    participant VM as ViewModel
    participant M as Model (Person)
    participant CSV as pessoas.csv

    U->>V: preenche Nome, Idade, Email\nclica "+ Adicionar"
    V->>VM: addPerson(name, age, email)
    VM->>VM: valida campos
    alt campos inválidos
        VM-->>V: onMessage("Preenche todos os campos.")
    else válidos
        VM->>M: new Person(name, age, email)
        M-->>VM: Person com ID autogerado
        VM-->>V: onDataChanged(lista atualizada)
        VM-->>V: onMessage("Adicionado com ID #N")
        V->>V: refreshMainTable()
    end
```

---

## Fluxo — Gravar e Carregar CSV

```mermaid
sequenceDiagram
    actor U as Utilizador
    participant V as View
    participant VM as ViewModel
    participant CU as CsvUtil
    participant CSV as pessoas.csv

    U->>V: clica "💾 Gravar"
    V->>VM: saveToCSV()
    VM->>CU: write("pessoas.csv", people)
    CU->>CSV: escreve id,nome,idade,email
    CSV-->>CU: ok
    CU-->>VM: ok
    VM-->>V: onMessage("Gravado em pessoas.csv (N registos).")

    U->>V: clica "📂 Carregar"
    V->>VM: loadFromCSV()
    VM->>VM: Person.resetCounter()
    VM->>CU: read("pessoas.csv")
    CU->>CSV: lê linhas
    CSV-->>CU: linhas raw
    CU-->>VM: List<Person>
    VM-->>V: onDataChanged(lista)
    VM-->>V: onMessage("Carregados N registos.")
    V->>V: refreshMainTable()
```

---

## Fluxo — Pesquisa em tempo real

```mermaid
sequenceDiagram
    actor U as Utilizador
    participant V as View (DocumentListener)
    participant VM as ViewModel

    U->>V: digita no campo de pesquisa
    V->>V: DocumentListener.insertUpdate / removeUpdate
    V->>VM: search(query)

    alt query é número inteiro
        VM->>VM: filtra por ID exacto
    else query é texto
        VM->>VM: filtra por Nome (contains, case-insensitive)
    end

    alt há resultados
        VM-->>V: onSearchResult(lista filtrada)
        V->>V: mostra painel de resultados\nrefreshSearchTable()
    else sem resultados ou query vazia
        VM-->>V: onSearchResult(lista vazia)
        V->>V: esconde painel de resultados
    end
```

---

## Padrão MVVM aplicado

| Camada        | Classe                | Responsabilidade                              |
|---------------|-----------------------|-----------------------------------------------|
| **Model**     | `Person`              | Dados puros — ID, nome, idade, email          |
| **ViewModel** | `PersonViewModel`     | Lógica + estado + callbacks → nunca toca em Swing |
| **View**      | `MainWindow`          | UI pura — chama ViewModel, recebe callbacks   |
| **View**      | `UIComponents`        | Componentes G2D customizados (botões, campos) |
| **Util**      | `CsvUtil`             | I/O de ficheiro CSV — sem conhecimento de UI  |

---

## Como compilar e correr

### Terminal (sem IDE)

```bash
# 1. Entrar na pasta src
cd src

# 2. Compilar todos os ficheiros
javac -d ../out $(find . -name "*.java")

# 3. Correr
cd ../out
java com.mvvmcsv.Main
```

### Eclipse
1. `File → New → Java Project`
2. Copia os ficheiros para `src/`
3. Cria os packages: `com.mvvmcsv`, `com.mvvmcsv.model`, `com.mvvmcsv.viewmodel`, `com.mvvmcsv.view`, `com.mvvmcsv.util`
4. `Run As → Java Application → Main`

### IntelliJ IDEA
1. `File → Open` → seleciona a pasta `mvvm-csv`
2. Marca `src` como *Sources Root*
3. `Run → Edit Configurations → Main class: com.mvvmcsv.Main`

---

## Formato do CSV

```
id,nome,idade,email
1,Susana Jesus,19,susana.jesus95@sapo.pt
2,Nuno Alves,26,nuno.alves95@gmail.com
```

> O ficheiro `pessoas.csv` é criado/lido na **raiz do projeto** (pasta onde corres o programa).