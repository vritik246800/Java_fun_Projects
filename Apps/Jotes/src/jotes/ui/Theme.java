package jotes.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tema visual da aplicação: estilo Apple Notes, desenhado à mão com {@link Graphics2D}
 * ("G2"), em duas paletas — escura ({@link ThemeColors#DARK}) e clara
 * ({@link ThemeColors#LIGHT}). Não usa FlatLaf — {@link #applyGlobal()} instala o
 * Look and Feel base (Metal) e sobrepõe todos os defaults do {@link UIManager} com a
 * paleta ativa, e regista {@link G2ButtonUI} e {@link DarkScrollBarUI} como UIs globais.
 * <p>As cores são campos estáticos <b>mutáveis</b>: {@link #setDark(boolean)} troca a
 * paleta e re-atribui todos os campos, pelo que os componentes que os leem durante a
 * pintura passam a usar as novas cores no repaint seguinte; {@code setDark} também
 * reaplica os defaults de cor do {@link UIManager} e chama
 * {@link SwingUtilities#updateComponentTreeUI(java.awt.Component)} em todas as janelas
 * abertas. A paleta ativa lê-se com {@link #current()} / {@link #isDark()}.</p>
 * <p>Para componentes desenhados à mão usar as constantes de cor, {@link #RADIUS},
 * {@link #roundedBorder()}, {@link RoundedPanel} e os stylers {@link #styleButton},
 * {@link #styleToolbarButton}, {@link #styleTextField}, {@link #styleScrollPane} e
 * {@link #styleSplitPane}.</p>
 */
public final class Theme {
    /** Raio de canto padrão de toda a aplicação. */
    public static final int RADIUS = 14;

    // ------------------------------------------------------------------
    // Cores da paleta ativa (re-atribuídas por setDark; ver ThemeColors)
    // ------------------------------------------------------------------
    /** Fundo principal da janela e do editor. */
    public static Color BG            = ThemeColors.DARK.bg();
    /** Fundo da sidebar (pastas/tags). */
    public static Color SIDEBAR_BG    = ThemeColors.DARK.sidebarBg();
    /** Fundo da lista de notas. */
    public static Color LIST_BG       = ThemeColors.DARK.listBg();
    /** Fundo de cartões, campos e botões. */
    public static Color CARD_BG       = ThemeColors.DARK.cardBg();
    /** Fundo de cartões/botões em hover. */
    public static Color CARD_HOVER    = ThemeColors.DARK.cardHover();
    /** Fundo de itens selecionados (pill da sidebar, cartão selecionado). */
    public static Color SELECTION     = ThemeColors.DARK.selection();
    /** Fundo de campos de texto preenchidos. */
    public static Color FIELD_BG      = ThemeColors.DARK.fieldBg();
    /** Linhas separadoras e contornos discretos. */
    public static Color SEPARATOR     = ThemeColors.DARK.separator();
    /** Texto principal. */
    public static Color TEXT          = ThemeColors.DARK.text();
    /** Texto secundário (datas, previews, hints). */
    public static Color TEXT_DIM      = ThemeColors.DARK.textDim();
    /** Acento amarelo-dourado do Apple Notes. */
    public static Color ACCENT        = ThemeColors.DARK.accent();
    /** Acento em hover (ligeiramente mais escuro). */
    public static Color ACCENT_HOVER  = ThemeColors.DARK.accentHover();
    /** Acento premido (ainda mais escuro). */
    public static Color ACCENT_PRESSED = ThemeColors.DARK.accentPressed();
    /** Acento translúcido para realces de fundo. */
    public static Color ACCENT_BG     = ThemeColors.DARK.accentBg();
    /** Ações destrutivas. */
    public static Color DANGER        = ThemeColors.DARK.danger();
    /** Fundo de texto selecionado dentro de campos. */
    public static Color TEXT_SELECTION = ThemeColors.DARK.textSelection();

    /** Paleta atualmente ativa. */
    private static ThemeColors current = ThemeColors.DARK;

    private Theme() {}

    /** A paleta ativa (origem de todas as cores dos campos públicos acima). */
    public static ThemeColors current() {
        return current;
    }

    /** {@code true} se o tema escuro estiver ativo. */
    public static boolean isDark() {
        return current == ThemeColors.DARK;
    }

    /**
     * Troca entre a paleta escura e a clara: re-atribui os campos de cor, reaplica os
     * defaults de cor do {@link UIManager} e atualiza
     * ({@link SwingUtilities#updateComponentTreeUI(java.awt.Component)}) e repinta todas
     * as janelas abertas. Não faz nada se a paleta pedida já estiver ativa.
     */
    public static void setDark(boolean dark) {
        ThemeColors palette = dark ? ThemeColors.DARK : ThemeColors.LIGHT;
        if (current == palette) return;
        current = palette;
        apply(palette);
        applyUiDefaults();
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            w.repaint();
        }
    }

    /** Re-atribui todos os campos de cor a partir da paleta indicada. */
    private static void apply(ThemeColors p) {
        BG = p.bg();
        SIDEBAR_BG = p.sidebarBg();
        LIST_BG = p.listBg();
        CARD_BG = p.cardBg();
        CARD_HOVER = p.cardHover();
        SELECTION = p.selection();
        FIELD_BG = p.fieldBg();
        SEPARATOR = p.separator();
        TEXT = p.text();
        TEXT_DIM = p.textDim();
        ACCENT = p.accent();
        ACCENT_HOVER = p.accentHover();
        ACCENT_PRESSED = p.accentPressed();
        ACCENT_BG = p.accentBg();
        DANGER = p.danger();
        TEXT_SELECTION = p.textSelection();
    }

    // ------------------------------------------------------------------
    // Tipografia
    // ------------------------------------------------------------------
    private static final String FONT_FAMILY = pickFontFamily();

    private static String pickFontFamily() {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String preferred : new String[]{"SF Pro Text", "San Francisco", "Segoe UI Variable Text",
                "Segoe UI", "Inter", "Roboto"}) {
            if (available.contains(preferred)) return preferred;
        }
        return "SansSerif";
    }

    /** Fonte da aplicação (estilo SF/Segoe) no estilo e tamanho pedidos. */
    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, Font.PLAIN, 13).deriveFont(style, size);
    }

    // ------------------------------------------------------------------
    // Setup global (sem FlatLaf)
    // ------------------------------------------------------------------
    /**
     * Instala o Look and Feel base e aplica a paleta ativa a todos os defaults do
     * {@link UIManager}. Chamar uma vez no arranque, antes de criar componentes
     * (depois de {@link #setDark(boolean)}, se houver uma preferência guardada).
     */
    public static void applyGlobal() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ignored) {
            // fica o L&F atual; os defaults da paleta aplicam-se na mesma
        }
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        // UIs desenhadas com G2
        UIManager.put("ButtonUI", G2ButtonUI.class.getName());
        UIManager.put("ToggleButtonUI", G2ButtonUI.class.getName());
        UIManager.put("ScrollBarUI", DarkScrollBarUI.class.getName());

        applyUiDefaults();

        // fonte da app em todo o lado
        FontUIResource baseFont = new FontUIResource(font(Font.PLAIN, 13));
        for (Object key : new ArrayList<>(UIManager.getDefaults().keySet())) {
            if (key != null && key.toString().toLowerCase().endsWith("font")) {
                UIManager.put(key, baseFont);
            }
        }
    }

    /**
     * (Re)aplica os defaults de cor do {@link UIManager} a partir da paleta ativa.
     * Chamado por {@link #applyGlobal()} no arranque e por {@link #setDark(boolean)}
     * em cada troca de tema.
     */
    private static void applyUiDefaults() {
        ColorUIResource bg = new ColorUIResource(BG);
        ColorUIResource card = new ColorUIResource(CARD_BG);
        ColorUIResource text = new ColorUIResource(TEXT);
        ColorUIResource dim = new ColorUIResource(TEXT_DIM);
        ColorUIResource selection = new ColorUIResource(SELECTION);
        ColorUIResource separator = new ColorUIResource(SEPARATOR);

        // contentores e texto
        UIManager.put("Panel.background", bg);
        UIManager.put("Panel.foreground", text);
        UIManager.put("Label.foreground", text);
        UIManager.put("Label.disabledForeground", dim);
        UIManager.put("Viewport.background", bg);
        UIManager.put("ScrollPane.background", bg);
        UIManager.put("SplitPane.background", bg);
        UIManager.put("ToolBar.background", bg);
        UIManager.put("ToolBar.foreground", text);
        UIManager.put("ToolBar.border", BorderFactory.createEmptyBorder());
        UIManager.put("Separator.foreground", separator);
        UIManager.put("Separator.background", bg);
        UIManager.put("TitledBorder.titleColor", dim);

        // botões (pintados por G2ButtonUI; estas cores são o estado normal)
        UIManager.put("Button.background", card);
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.select", selection);
        UIManager.put("Button.disabledText", dim);
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(6, 14, 6, 14));
        UIManager.put("Button.margin", new Insets(2, 8, 2, 8));
        UIManager.put("ToggleButton.background", card);
        UIManager.put("ToggleButton.foreground", text);
        UIManager.put("ToggleButton.select", selection);
        UIManager.put("CheckBox.background", bg);
        UIManager.put("CheckBox.foreground", text);
        UIManager.put("RadioButton.background", bg);
        UIManager.put("RadioButton.foreground", text);

        // campos de texto
        for (String key : new String[]{"TextField", "FormattedTextField", "PasswordField"}) {
            UIManager.put(key + ".background", card);
            UIManager.put(key + ".foreground", text);
            UIManager.put(key + ".caretForeground", text);
            UIManager.put(key + ".selectionBackground", new ColorUIResource(TEXT_SELECTION));
            UIManager.put(key + ".selectionForeground", text);
            UIManager.put(key + ".inactiveForeground", dim);
        }
        for (String key : new String[]{"TextArea", "TextPane", "EditorPane"}) {
            UIManager.put(key + ".background", bg);
            UIManager.put(key + ".foreground", text);
            UIManager.put(key + ".caretForeground", text);
            UIManager.put(key + ".selectionBackground", new ColorUIResource(TEXT_SELECTION));
            UIManager.put(key + ".selectionForeground", text);
            UIManager.put(key + ".inactiveForeground", dim);
        }

        // listas, tabelas, árvores
        UIManager.put("List.background", new ColorUIResource(LIST_BG));
        UIManager.put("List.foreground", text);
        UIManager.put("List.selectionBackground", selection);
        UIManager.put("List.selectionForeground", text);
        UIManager.put("Table.background", card);
        UIManager.put("Table.foreground", text);
        UIManager.put("Table.selectionBackground", selection);
        UIManager.put("Table.selectionForeground", text);
        UIManager.put("Table.gridColor", separator);
        UIManager.put("TableHeader.background", bg);
        UIManager.put("TableHeader.foreground", dim);
        UIManager.put("Tree.background", new ColorUIResource(SIDEBAR_BG));
        UIManager.put("Tree.foreground", text);
        UIManager.put("Tree.selectionBackground", selection);
        UIManager.put("Tree.selectionForeground", text);
        UIManager.put("Tree.textBackground", new ColorUIResource(SIDEBAR_BG));

        // combo/spinner
        UIManager.put("ComboBox.background", card);
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.selectionBackground", selection);
        UIManager.put("ComboBox.selectionForeground", text);
        UIManager.put("ComboBox.buttonBackground", card);
        UIManager.put("ComboBox.buttonDarkShadow", separator);
        UIManager.put("ComboBox.buttonHighlight", card);
        UIManager.put("ComboBox.buttonShadow", separator);
        UIManager.put("Spinner.background", card);
        UIManager.put("Spinner.foreground", text);

        // menus e popups
        UIManager.put("MenuBar.background", new ColorUIResource(SIDEBAR_BG));
        UIManager.put("MenuBar.foreground", text);
        UIManager.put("PopupMenu.background", card);
        UIManager.put("PopupMenu.foreground", text);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(SEPARATOR));
        for (String key : new String[]{"Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem"}) {
            UIManager.put(key + ".background", card);
            UIManager.put(key + ".foreground", text);
            UIManager.put(key + ".selectionBackground", selection);
            UIManager.put(key + ".selectionForeground", text);
            UIManager.put(key + ".disabledForeground", dim);
        }

        // diálogos, tooltips, progresso
        UIManager.put("OptionPane.background", bg);
        UIManager.put("OptionPane.foreground", text);
        UIManager.put("OptionPane.messageForeground", text);
        UIManager.put("ToolTip.background", card);
        UIManager.put("ToolTip.foreground", text);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(SEPARATOR));
        UIManager.put("ProgressBar.background", card);
        UIManager.put("ProgressBar.foreground", new ColorUIResource(ACCENT));
        UIManager.put("ProgressBar.selectionBackground", bg);
        UIManager.put("ProgressBar.selectionForeground", bg);
        UIManager.put("Slider.background", bg);
        UIManager.put("Slider.foreground", dim);
        UIManager.put("ScrollBar.background", new ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("ScrollBar.foreground", dim);
        UIManager.put("ScrollBar.thumb", new ColorUIResource(CARD_HOVER));
        UIManager.put("ScrollBar.track", new ColorUIResource(new Color(0, 0, 0, 0)));
    }

    // ------------------------------------------------------------------
    // Bordas e painéis arredondados
    // ------------------------------------------------------------------
    public static Border roundedBorder() {
        return new RoundedBorder(borderColor(), 1, RADIUS);
    }

    public static Border roundedBorder(Color color, int thickness, int radius) {
        return new RoundedBorder(color, thickness, radius);
    }

    /** Borda arredondada com padding interior. */
    public static Border roundedBorderWithPadding(int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
                roundedBorder(), BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    public static Color borderColor() {
        return SEPARATOR;
    }

    public static Color accent() {
        return ACCENT;
    }

    public static Color dimForeground() {
        return TEXT_DIM;
    }

    // ------------------------------------------------------------------
    // Stylers de componentes
    // ------------------------------------------------------------------
    /** Botão arredondado: fundo {@link #CARD_BG}, hover {@link #CARD_HOVER}. */
    public static void styleButton(AbstractButton b) {
        b.setUI(new G2ButtonUI());
        b.setBackground(CARD_BG);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFont(font(Font.PLAIN, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Botão de toolbar: transparente em repouso, pill em hover. */
    public static void styleToolbarButton(AbstractButton b) {
        styleButton(b);
        b.setBackground(new Color(0, 0, 0, 0));
        b.setForeground(TEXT_DIM);
        b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    /** Botão primário: pill na cor de acento com texto escuro (ação principal do painel). */
    public static void stylePrimaryButton(AbstractButton b) {
        styleButton(b);
        b.setBackground(ACCENT);
        b.setForeground(current.onAccent());
        b.setFont(font(Font.BOLD, 13));
    }

    /**
     * Campo de texto: cores/caret/seleção do tema e borda arredondada com padding.
     * O componente fica não-opaco (fundo do pai visível); para um campo com fundo
     * "pill" preenchido e placeholder usar {@link PlaceholderField}.
     */
    public static void styleTextField(JTextComponent tc) {
        tc.setBackground(FIELD_BG);
        tc.setForeground(TEXT);
        tc.setCaretColor(TEXT);
        tc.setSelectionColor(TEXT_SELECTION);
        tc.setSelectedTextColor(TEXT);
        tc.setOpaque(false);
        tc.setBorder(BorderFactory.createCompoundBorder(
                roundedBorder(SEPARATOR, 1, 10), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tc.setFont(font(Font.PLAIN, 13));
    }

    /** ScrollPane sem borda, com scrollbars finos e arredondados. */
    public static void styleScrollPane(JScrollPane sp) {
        sp.setBorder(null);
        sp.setOpaque(false);
        if (sp.getViewport() != null) sp.getViewport().setOpaque(false);
        JScrollBar vertical = sp.getVerticalScrollBar();
        if (vertical != null) {
            vertical.setUI(new DarkScrollBarUI());
            vertical.setUnitIncrement(16);
        }
        JScrollBar horizontal = sp.getHorizontalScrollBar();
        if (horizontal != null) {
            horizontal.setUI(new DarkScrollBarUI());
            horizontal.setUnitIncrement(16);
        }
    }

    /** SplitPane com divisor fino de 1px na cor {@link #SEPARATOR}, sem bordas. */
    public static void styleSplitPane(JSplitPane sp) {
        sp.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    {
                        setBorder(null);
                        setBackground(SEPARATOR);
                    }

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(SEPARATOR);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });
        sp.setDividerSize(1);
        sp.setBorder(null);
        sp.setBackground(BG);
        sp.setContinuousLayout(true);
    }

    // ------------------------------------------------------------------
    // Classes auxiliares
    // ------------------------------------------------------------------
    /** Borda simples desenhada como retângulo arredondado. */
    public static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color != null ? color : borderColor());
            for (int i = 0; i < thickness; i++) {
                g2.drawRoundRect(x + i, y + i, width - 1 - 2 * i, height - 1 - 2 * i, radius, radius);
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            int pad = Math.max(2, radius / 4);
            insets.set(thickness + pad, thickness + pad, thickness + pad, thickness + pad);
            return insets;
        }

        @Override
        public boolean isBorderOpaque() { return false; }
    }

    /** Painel com fundo arredondado (não opaco, desenha o próprio fundo). */
    public static class RoundedPanel extends JPanel {
        private int radius = RADIUS;
        private Color fill;

        public RoundedPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        public void setCornerRadius(int radius) { this.radius = radius; repaint(); }

        /** Cor de preenchimento; por omissão usa o background do painel. */
        public void setFill(Color fill) { this.fill = fill; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill != null ? fill : getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Campo de texto com fundo "pill" arredondado e placeholder pintado com G2
     * (substitui as propriedades FlatLaf {@code JTextField.placeholderText/padding}).
     */
    public static class PlaceholderField extends JTextField {
        private String placeholder;

        public PlaceholderField() {
            this("");
        }

        public PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
            styleTextField(this);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            repaint();
        }

        public String getPlaceholder() {
            return placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && placeholder != null && !placeholder.isEmpty()) {
                Graphics2D ph = (Graphics2D) g.create();
                ph.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                ph.setColor(TEXT_DIM);
                ph.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = ph.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                ph.drawString(placeholder, in.left, y);
                ph.dispose();
            }
        }
    }

    /**
     * UI de botão pintada com G2: pill arredondada com estados hover/pressed/selected.
     * Registada globalmente como {@code ButtonUI} e {@code ToggleButtonUI}.
     */
    public static class G2ButtonUI extends BasicButtonUI {
        @SuppressWarnings("unused")
        public static ComponentUI createUI(JComponent c) {
            return new G2ButtonUI();
        }

        @Override
        public void update(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel m = b.getModel();
            boolean accent = ACCENT.equals(b.getBackground());
            Color fill = b.getBackground();
            if (m.isPressed() || m.isSelected()) {
                fill = accent ? ACCENT_PRESSED : SELECTION;
            } else if (m.isRollover()) {
                fill = accent ? ACCENT_HOVER : CARD_HOVER;
            }
            if (b.isContentAreaFilled() && fill != null && fill.getAlpha() > 0) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), RADIUS, RADIUS);
            }
            g2.dispose();
            paint(g, c);
        }

        @Override
        protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                                  Rectangle textRect, Rectangle iconRect) {
            // sem anel de foco — o feedback é dado pelo hover
        }
    }

    /**
     * Scrollbar estilo overlay: track invisível, thumb pill arredondado,
     * sem botões de seta. Registada globalmente como {@code ScrollBarUI}.
     */
    public static class DarkScrollBarUI extends BasicScrollBarUI {
        @SuppressWarnings("unused")
        public static ComponentUI createUI(JComponent c) {
            return new DarkScrollBarUI();
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = current.scrollThumb();
            thumbDarkShadowColor = thumbColor;
            thumbHighlightColor = thumbColor;
            thumbLightShadowColor = thumbColor;
            trackColor = new Color(0, 0, 0, 0);
            trackHighlightColor = trackColor;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return emptyButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return emptyButton();
        }

        private static JButton emptyButton() {
            JButton b = new JButton();
            Dimension zero = new Dimension(0, 0);
            b.setPreferredSize(zero);
            b.setMinimumSize(zero);
            b.setMaximumSize(zero);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // track invisível — estilo overlay
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? current.scrollThumbHover() : current.scrollThumb());
            int arc = Math.min(thumbBounds.width, thumbBounds.height);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1,
                    thumbBounds.width - 2, thumbBounds.height - 2, arc, arc);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            JScrollBar bar = (JScrollBar) c;
            return bar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(10, 48) : new Dimension(48, 10);
        }
    }
}
