# GUIA COMPLETO — Java Swing (GUI Puro)

---

## Índice

1. [Estrutura Base](#1-estrutura-base)
2. [JFrame — Janela Principal](#2-jframe--janela-principal)
3. [Layouts — Todos os Disponíveis](#3-layouts--todos-os-disponíveis)
4. [Menu (JMenuBar / JMenu / JMenuItem)](#4-menu-jmenubar--jmenu--jmenuitem)
5. [Componentes — Referência Completa](#5-componentes--referência-completa)
6. [JTable — Tabela Completa](#6-jtable--tabela-completa)
7. [Eventos — Referência Completa](#7-eventos--referência-completa)
8. [JPopupMenu — Clique Direito](#8-jpopupmenu--clique-direito)
9. [Diálogos e Janelas Auxiliares](#9-diálogos-e-janelas-auxiliares)
10. [Boas Práticas e Estrutura Profissional](#10-boas-práticas-e-estrutura-profissional)

---

## 1. Estrutura Base

Sempre lançar a UI na EDT (Event Dispatch Thread):

```java
import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow());
    }
}
```

**Porquê `invokeLater`?**  
O Swing não é thread-safe. Toda a criação e modificação de componentes tem de correr na EDT. Sem isto, podem ocorrer erros visuais e race conditions.

---

## 2. JFrame — Janela Principal

```java
public class MainWindow extends JFrame {

    public MainWindow() {
        setTitle("Título da Janela");
        setSize(900, 600);
        setMinimumSize(new Dimension(400, 300));   // tamanho mínimo
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);               // centra no ecrã
        setResizable(true);

        setLayout(new BorderLayout());

        add(new TopPanel(), BorderLayout.NORTH);
        add(new CenterPanel(), BorderLayout.CENTER);
        add(new BottomPanel(), BorderLayout.SOUTH);

        setJMenuBar(new AppMenuBar());

        setVisible(true);
    }
}
```

**Opções de `setDefaultCloseOperation`:**

| Constante | Comportamento |
|---|---|
| `EXIT_ON_CLOSE` | Fecha a app |
| `DISPOSE_ON_CLOSE` | Liberta recursos, não fecha a JVM |
| `HIDE_ON_CLOSE` | Esconde a janela |
| `DO_NOTHING_ON_CLOSE` | Ignora — controlo manual |

---

## 3. Layouts — Todos os Disponíveis

### 3.1 BorderLayout *(mais usado para janelas principais)*

Divide o container em 5 zonas: NORTH, SOUTH, EAST, WEST, CENTER.

```java
panel.setLayout(new BorderLayout());
panel.setLayout(new BorderLayout(10, 5)); // hgap, vgap

panel.add(comp, BorderLayout.NORTH);
panel.add(comp, BorderLayout.SOUTH);
panel.add(comp, BorderLayout.EAST);
panel.add(comp, BorderLayout.WEST);
panel.add(comp, BorderLayout.CENTER); // expande para ocupar espaço restante
```

```
┌──────────────────────────┐
│          NORTH           │
├──────┬───────────┬───────┤
│ WEST │  CENTER   │  EAST │
├──────┴───────────┴───────┤
│          SOUTH           │
└──────────────────────────┘
```

---

### 3.2 FlowLayout *(botões e inputs em linha)*

Alinha componentes horizontalmente, passa para linha seguinte quando não cabe.

```java
panel.setLayout(new FlowLayout());
panel.setLayout(new FlowLayout(FlowLayout.LEFT));
panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5)); // align, hgap, vgap
```

**Alinhamentos:** `FlowLayout.LEFT`, `RIGHT`, `CENTER`, `LEADING`, `TRAILING`

```
[ Botão A ] [ Botão B ] [ Botão C ]
[ Botão D ] [ Botão E ]
```

---

### 3.3 GridLayout *(grelha uniforme)*

Todos os componentes têm o mesmo tamanho. Útil para teclados, calculadoras, formulários simples.

```java
panel.setLayout(new GridLayout(3, 2));          // 3 linhas, 2 colunas
panel.setLayout(new GridLayout(3, 2, 10, 10));  // + hgap, vgap
panel.setLayout(new GridLayout(0, 2));          // 0 linhas = ilimitado
```

```
┌──────┬──────┐
│  C1  │  C2  │
├──────┼──────┤
│  C3  │  C4  │
├──────┼──────┤
│  C5  │  C6  │
└──────┴──────┘
```

---

### 3.4 GridBagLayout *(layout mais poderoso e flexível)*

Controlo total: posição, span de colunas/linhas, peso, padding, ancoragem.

```java
panel.setLayout(new GridBagLayout());
GridBagConstraints c = new GridBagConstraints();

// Posição
c.gridx = 0;        // coluna
c.gridy = 0;        // linha

// Span
c.gridwidth = 2;    // ocupa 2 colunas
c.gridheight = 1;   // ocupa 1 linha

// Expansão
c.weightx = 1.0;    // expande horizontalmente
c.weighty = 1.0;    // expande verticalmente
c.fill = GridBagConstraints.HORIZONTAL; // como preenche o espaço

// Ancoragem (quando não preenche todo o espaço)
c.anchor = GridBagConstraints.NORTHWEST;

// Padding externo e interno
c.insets = new Insets(5, 5, 5, 5); // top, left, bottom, right
c.ipadx = 10;
c.ipady = 5;

panel.add(componente, c);
```

**Valores de `fill`:**

| Valor | Efeito |
|---|---|
| `NONE` | Não expande |
| `HORIZONTAL` | Expande horizontalmente |
| `VERTICAL` | Expande verticalmente |
| `BOTH` | Expande nas duas direções |

**Valores de `anchor`:**

`NORTH`, `SOUTH`, `EAST`, `WEST`, `CENTER`, `NORTHEAST`, `NORTHWEST`, `SOUTHEAST`, `SOUTHWEST`, `FIRST_LINE_START`, `PAGE_END`, etc.

---

### 3.5 BoxLayout *(pilha horizontal ou vertical)*

Componentes em linha ou coluna, com tamanhos variáveis.

```java
panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // vertical
panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS)); // horizontal
```

**Espaçamento com Box:**

```java
panel.add(Box.createRigidArea(new Dimension(0, 10)));   // espaço fixo
panel.add(Box.createVerticalStrut(10));                  // espaço vertical fixo
panel.add(Box.createHorizontalStrut(10));                // espaço horizontal fixo
panel.add(Box.createVerticalGlue());                     // espaço flexível vertical
panel.add(Box.createHorizontalGlue());                   // espaço flexível horizontal
```

**Alinhamento de componentes:**

```java
comp.setAlignmentX(Component.LEFT_ALIGNMENT);
comp.setAlignmentX(Component.CENTER_ALIGNMENT);
comp.setAlignmentX(Component.RIGHT_ALIGNMENT);
```

---

### 3.6 CardLayout *(painéis "em stack" — tipo wizard/abas)*

Mostra um painel de cada vez, útil para navegação entre "páginas".

```java
CardLayout card = new CardLayout();
JPanel container = new JPanel(card);

container.add(new PaginaA(), "pagina_a");
container.add(new PaginaB(), "pagina_b");

// Navegar
card.show(container, "pagina_b");
card.first(container);
card.last(container);
card.next(container);
card.previous(container);
```

---

### 3.7 SpringLayout *(posicionamento relativo entre componentes)*

Define relações de posição entre componentes. Poderoso mas verboso.

```java
SpringLayout layout = new SpringLayout();
panel.setLayout(layout);

JLabel label = new JLabel("Nome:");
JTextField field = new JTextField(15);
panel.add(label);
panel.add(field);

// field fica 5px à direita do label
layout.putConstraint(SpringLayout.WEST, field, 5, SpringLayout.EAST, label);

// ambos ficam 10px do topo do container
layout.putConstraint(SpringLayout.NORTH, label, 10, SpringLayout.NORTH, panel);
layout.putConstraint(SpringLayout.NORTH, field, 10, SpringLayout.NORTH, panel);
```

**Constantes de posição:** `NORTH`, `SOUTH`, `EAST`, `WEST`, `HORIZONTAL_CENTER`, `VERTICAL_CENTER`, `BASELINE`

---

### 3.8 null Layout *(posicionamento absoluto — evitar em UIs redimensionáveis)*

Posição e tamanho definidos manualmente.

```java
panel.setLayout(null);

JButton btn = new JButton("Clica");
btn.setBounds(50, 100, 120, 30); // x, y, largura, altura
panel.add(btn);
```

> ⚠️ Não se adapta a diferentes resoluções nem a redimensionamento da janela. Usar apenas em protótipos ou UIs de tamanho fixo.

---

### 3.9 GroupLayout *(usado pelo NetBeans GUI Builder)*

Controlo preciso de grupos horizontais e verticais. Geralmente gerado por ferramentas, mas pode ser escrito manualmente.

```java
GroupLayout layout = new GroupLayout(panel);
panel.setLayout(layout);

layout.setAutoCreateGaps(true);
layout.setAutoCreateContainerGaps(true);

layout.setHorizontalGroup(
    layout.createSequentialGroup()
        .addComponent(label)
        .addComponent(field)
);

layout.setVerticalGroup(
    layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(label)
        .addComponent(field)
);
```

---

### 3.10 Resumo Comparativo de Layouts

| Layout | Uso Ideal | Redimensiona? | Complexidade |
|---|---|---|---|
| `BorderLayout` | Estrutura principal da janela | ✅ Sim | Baixa |
| `FlowLayout` | Botões em linha | ⚠️ Parcial | Baixa |
| `GridLayout` | Grelhas uniformes | ✅ Sim | Baixa |
| `GridBagLayout` | Formulários complexos | ✅ Sim | Alta |
| `BoxLayout` | Listas verticais/horizontais | ✅ Sim | Média |
| `CardLayout` | Wizard / multi-página | ✅ Sim | Média |
| `SpringLayout` | Posicionamento relativo | ✅ Sim | Alta |
| `GroupLayout` | UI gerada por ferramentas | ✅ Sim | Alta |
| `null` | Posição absoluta fixa | ❌ Não | Baixa |

---

## 4. Menu (JMenuBar / JMenu / JMenuItem)

```java
class AppMenuBar extends JMenuBar {

    public AppMenuBar() {
        JMenu menuFicheiro = new JMenu("Ficheiro");
        JMenu menuEditar   = new JMenu("Editar");
        JMenu menuAjuda    = new JMenu("Ajuda");

        // Item simples
        JMenuItem novo  = new JMenuItem("Novo");
        JMenuItem abrir = new JMenuItem("Abrir");
        JMenuItem sair  = new JMenuItem("Sair");

        // Com atalho de teclado
        novo.setAccelerator(KeyStroke.getKeyStroke("ctrl N"));
        abrir.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));

        // CheckBox no menu
        JCheckBoxMenuItem mostrarBarra = new JCheckBoxMenuItem("Mostrar Barra", true);

        // Radio no menu
        JRadioButtonMenuItem opcaoA = new JRadioButtonMenuItem("Opção A", true);
        JRadioButtonMenuItem opcaoB = new JRadioButtonMenuItem("Opção B");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(opcaoA);
        grupo.add(opcaoB);

        // Submenu
        JMenu submenu = new JMenu("Exportar");
        submenu.add(new JMenuItem("PDF"));
        submenu.add(new JMenuItem("CSV"));

        // Eventos
        sair.addActionListener(e -> System.exit(0));
        novo.addActionListener(e -> System.out.println("Novo"));

        // Montar menu
        menuFicheiro.add(novo);
        menuFicheiro.add(abrir);
        menuFicheiro.add(submenu);
        menuFicheiro.addSeparator();
        menuFicheiro.add(sair);

        menuEditar.add(mostrarBarra);
        menuEditar.addSeparator();
        menuEditar.add(opcaoA);
        menuEditar.add(opcaoB);

        add(menuFicheiro);
        add(menuEditar);
        add(menuAjuda);
    }
}
```

---

## 5. Componentes — Referência Completa

### 5.1 Texto

```java
JLabel label = new JLabel("Texto");
label.setForeground(Color.BLUE);
label.setFont(new Font("Arial", Font.BOLD, 14));
label.setIcon(new ImageIcon("icone.png")); // label com ícone

JTextField campo = new JTextField(20);       // largura em colunas
campo.setText("valor");
campo.getText();
campo.setEditable(false);
campo.setToolTipText("Escreve o nome aqui");

JTextArea area = new JTextArea(5, 30);       // linhas, colunas
area.setLineWrap(true);
area.setWrapStyleWord(true);
JScrollPane scroll = new JScrollPane(area);  // sempre envolver em scroll

JPasswordField pass = new JPasswordField(20);
char[] senha = pass.getPassword(); // nunca usar getText() em passwords
```

---

### 5.2 Botões

```java
JButton btn = new JButton("Clica");
btn.setEnabled(false);
btn.setMnemonic(KeyEvent.VK_C); // Alt+C ativa o botão
btn.setToolTipText("Clica para confirmar");

JToggleButton toggle = new JToggleButton("Ativo");
toggle.addActionListener(e -> {
    System.out.println("Estado: " + toggle.isSelected());
});
```

---

### 5.3 Checkboxes e RadioButtons

```java
JCheckBox check = new JCheckBox("Aceito os termos");
check.setSelected(true);
check.addActionListener(e -> System.out.println(check.isSelected()));

JRadioButton r1 = new JRadioButton("Opção A");
JRadioButton r2 = new JRadioButton("Opção B");
JRadioButton r3 = new JRadioButton("Opção C");

ButtonGroup grupo = new ButtonGroup(); // só uma pode estar selecionada
grupo.add(r1);
grupo.add(r2);
grupo.add(r3);
r1.setSelected(true);
```

---

### 5.4 ComboBox e List

```java
// ComboBox (dropdown)
String[] opcoes = {"Janeiro", "Fevereiro", "Março"};
JComboBox<String> combo = new JComboBox<>(opcoes);
combo.addItem("Abril");
combo.getSelectedItem();
combo.setEditable(true); // permite escrever valor livre
combo.addActionListener(e -> System.out.println(combo.getSelectedItem()));

// JList
DefaultListModel<String> listModel = new DefaultListModel<>();
listModel.addElement("Item 1");
listModel.addElement("Item 2");
JList<String> lista = new JList<>(listModel);
lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
JScrollPane scrollLista = new JScrollPane(lista);

lista.addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting()) {
        System.out.println(lista.getSelectedValue());
    }
});
```

---

### 5.5 Sliders, Spinners e ProgressBar

```java
// Slider
JSlider slider = new JSlider(0, 100, 50); // min, max, valor inicial
slider.setMajorTickSpacing(20);
slider.setMinorTickSpacing(5);
slider.setPaintTicks(true);
slider.setPaintLabels(true);
slider.addChangeListener(e -> System.out.println(slider.getValue()));

// Spinner
SpinnerNumberModel spinModel = new SpinnerNumberModel(0, 0, 100, 1); // val, min, max, step
JSpinner spinner = new JSpinner(spinModel);
spinner.getValue();

// ProgressBar
JProgressBar progress = new JProgressBar(0, 100);
progress.setValue(75);
progress.setStringPainted(true);
progress.setString("75%"); // texto personalizado
progress.setIndeterminate(true); // animação contínua (loading)
```

---

### 5.6 Containers

```java
// Painel com borda e título
JPanel panel = new JPanel();
panel.setBorder(BorderFactory.createTitledBorder("Secção"));
panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // padding

// Scroll
JScrollPane scroll = new JScrollPane(componente);
scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

// SplitPane
JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelEsq, painelDir);
split.setDividerLocation(300); // posição do divisor em px
split.setResizeWeight(0.3);    // 30% para o esquerdo ao redimensionar

// TabbedPane
JTabbedPane tabs = new JTabbedPane();
tabs.addTab("Geral", new ImageIcon("icone.png"), new PainelGeral(), "Tooltip");
tabs.addTab("Avançado", new PainelAvancado());
tabs.setSelectedIndex(0);
tabs.addChangeListener(e -> System.out.println("Tab: " + tabs.getSelectedIndex()));
```

---

### 5.7 Choosers

```java
// File Chooser
JFileChooser fc = new JFileChooser();
fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens", "jpg", "png"));
int resultado = fc.showOpenDialog(frame);   // ou showSaveDialog
if (resultado == JFileChooser.APPROVE_OPTION) {
    File ficheiro = fc.getSelectedFile();
}

// Color Chooser
Color cor = JColorChooser.showDialog(frame, "Escolhe uma cor", Color.RED);
```

---

## 6. JTable — Tabela Completa

```java
import javax.swing.table.DefaultTableModel;

String[] colunas = {"ID", "Nome", "Email"};
DefaultTableModel model = new DefaultTableModel(colunas, 0) {
    // tornar coluna não editável
    @Override
    public boolean isCellEditable(int row, int col) {
        return col != 0; // ID não editável
    }
};

JTable tabela = new JTable(model);

// Configuração visual
tabela.setRowHeight(25);
tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
tabela.getTableHeader().setReorderingAllowed(false);
tabela.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

// Largura de colunas
tabela.getColumnModel().getColumn(0).setPreferredWidth(50);
tabela.getColumnModel().getColumn(1).setPreferredWidth(150);

// Ordenação automática
tabela.setAutoCreateRowSorter(true);

// Adicionar linha
model.addRow(new Object[]{1, "Ana", "ana@email.com"});

// Remover linha selecionada
int row = tabela.getSelectedRow();
if (row != -1) {
    int modelRow = tabela.convertRowIndexToModel(row); // importante quando há sort
    model.removeRow(modelRow);
}

// Ler valor de célula
String nome = (String) model.getValueAt(row, 1);

// Alterar valor de célula
model.setValueAt("Novo Nome", row, 1);

// Limpar tabela
model.setRowCount(0);

// Sempre em JScrollPane
JScrollPane scroll = new JScrollPane(tabela);
```

---

## 7. Eventos — Referência Completa

### ActionListener (botões, menu, combo, enter em textfield)

```java
// Lambda
btn.addActionListener(e -> System.out.println("Clicado"));

// Classe anónima
btn.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        System.out.println("Clicado");
    }
});
```

---

### KeyListener

```java
campo.addKeyListener(new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            System.out.println("Enter pressionado");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
});
```

---

### MouseListener

```java
comp.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            System.out.println("Duplo clique");
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            System.out.println("Clique direito");
        }
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e)  {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e){}
});
```

---

### FocusListener

```java
campo.addFocusListener(new FocusAdapter() {
    @Override
    public void focusGained(FocusEvent e) {
        campo.selectAll(); // seleciona texto ao entrar no campo
    }

    @Override
    public void focusLost(FocusEvent e) {
        System.out.println("Saiu do campo: " + campo.getText());
    }
});
```

---

### DocumentListener (mudança em tempo real no texto)

```java
campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
    public void insertUpdate(javax.swing.event.DocumentEvent e)  { onChange(); }
    public void removeUpdate(javax.swing.event.DocumentEvent e)  { onChange(); }
    public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange(); }

    void onChange() {
        System.out.println("Texto alterado: " + campo.getText());
    }
});
```

---

### ListSelectionListener (JTable e JList)

```java
tabela.getSelectionModel().addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting()) {
        int row = tabela.getSelectedRow();
        if (row != -1) {
            System.out.println("Linha selecionada: " + row);
        }
    }
});
```

---

### WindowListener

```java
frame.addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) {
        int resposta = JOptionPane.showConfirmDialog(frame,
            "Tens a certeza que queres sair?", "Sair",
            JOptionPane.YES_NO_OPTION);
        if (resposta == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
});
frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
```

---

## 8. JPopupMenu — Clique Direito

### Método recomendado (`setComponentPopupMenu`)

```java
JPopupMenu popup = new JPopupMenu();
JMenuItem editar = new JMenuItem("Editar");
JMenuItem apagar = new JMenuItem("Apagar");

popup.add(editar);
popup.addSeparator();
popup.add(apagar);

tabela.setComponentPopupMenu(popup);

editar.addActionListener(e -> {
    int row = tabela.getSelectedRow();
    if (row != -1) {
        System.out.println("Editar linha: " + row);
    }
});
```

### Método manual (mais controlo)

```java
tabela.addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            // selecionar linha sob o cursor antes de mostrar menu
            int row = tabela.rowAtPoint(e.getPoint());
            if (row >= 0) tabela.setRowSelectionInterval(row, row);
            popup.show(tabela, e.getX(), e.getY());
        }
    }
});
```

---

## 9. Diálogos e Janelas Auxiliares

### JOptionPane

```java
// Mensagem simples
JOptionPane.showMessageDialog(frame, "Operação concluída!");
JOptionPane.showMessageDialog(frame, "Erro!", "Título", JOptionPane.ERROR_MESSAGE);

// Tipos: INFORMATION_MESSAGE, WARNING_MESSAGE, ERROR_MESSAGE, QUESTION_MESSAGE, PLAIN_MESSAGE

// Confirmação
int r = JOptionPane.showConfirmDialog(frame, "Tens a certeza?");
// Retorna: YES_OPTION, NO_OPTION, CANCEL_OPTION, OK_OPTION, CLOSED_OPTION

// Input
String valor = JOptionPane.showInputDialog(frame, "Escreve o nome:");

// Opções personalizadas
String[] opcoes = {"Guardar", "Descartar", "Cancelar"};
int escolha = JOptionPane.showOptionDialog(frame,
    "Existem alterações não guardadas.",
    "Fechar",
    JOptionPane.YES_NO_CANCEL_OPTION,
    JOptionPane.WARNING_MESSAGE,
    null, opcoes, opcoes[0]);
```

---

### JDialog (janela personalizada)

```java
JDialog dialog = new JDialog(frame, "Título", true); // true = modal
dialog.setSize(400, 300);
dialog.setLocationRelativeTo(frame);
dialog.setLayout(new BorderLayout());

JPanel conteudo = new JPanel();
conteudo.add(new JLabel("Conteúdo aqui"));

JButton fechar = new JButton("Fechar");
fechar.addActionListener(e -> dialog.dispose());

dialog.add(conteudo, BorderLayout.CENTER);
dialog.add(fechar, BorderLayout.SOUTH);
dialog.setVisible(true);
```

---

## 10. Boas Práticas e Estrutura Profissional

### Separação de responsabilidades

```
MainWindow.java      → JFrame, estrutura geral
MenuBar.java         → JMenuBar
TopPanel.java        → Inputs e filtros
CenterPanel.java     → Tabela ou conteúdo principal
BottomPanel.java     → Barra de estado, botões de ação
```

### Padrão de inicialização

```java
public class MainWindow extends JFrame {

    private CenterPanel centerPanel;

    public MainWindow() {
        initComponents();
        initLayout();
        initEvents();
        setVisible(true);
    }

    private void initComponents() { /* criar componentes */ }
    private void initLayout()     { /* montar layout */ }
    private void initEvents()     { /* ligar eventos */ }
}
```

### Atualizar UI a partir de outra thread (ex: rede, base de dados)

```java
// NUNCA atualizar Swing fora da EDT
SwingUtilities.invokeLater(() -> {
    tableModel.addRow(new Object[]{1, "Dado carregado"});
});

// Para operações longas sem bloquear a UI: usar SwingWorker
SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
    @Override
    protected List<String> doInBackground() throws Exception {
        // corre em background thread
        return carregarDadosDaBaseDados();
    }

    @Override
    protected void done() {
        // corre na EDT após doInBackground terminar
        try {
            List<String> dados = get();
            // atualizar UI aqui
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
};
worker.execute();
```

### Dicas rápidas

- Sempre envolve `JTable` e `JTextArea` em `JScrollPane`
- Usa `convertRowIndexToModel()` ao remover linhas com sort ativo
- Usa `setAutoCreateRowSorter(true)` para ordenação grátis na tabela
- `ButtonGroup` não é um componente visual — é apenas um controlador de seleção
- `pack()` em vez de `setSize()` — redimensiona a janela ao conteúdo
- Usa `BorderFactory` para bordas em vez de criar objetos diretamente
- Evita `null` layout em UIs redimensionáveis

---

*Guia gerado como referência completa para desenvolvimento com Java Swing puro.*
