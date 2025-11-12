import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Botão moderno com animações suaves e efeitos visuais
 */
class BotaoModerno extends JButton {
    private static final int ANIMATION_DURATION = 200;
    private float hoverProgress = 0f;
    private float pressProgress = 0f;
    private Timer hoverTimer;
    private Timer pressTimer;
    private Color baseColor;
    private Color hoverColor;
    private Color pressColor;
    private boolean isHovered = false;
    private boolean isPressed = false;
    
    // Efeito de ondulação (ripple)
    private Point ripplePoint;
    private float rippleAlpha = 0f;
    private Timer rippleTimer;
    
    public BotaoModerno(String text, Color baseColor) {
        super(text);
        this.baseColor = baseColor;
        this.hoverColor = brighten(baseColor, 0.15f);
        this.pressColor = darken(baseColor, 0.1f);
        
        setupButton();
        setupAnimations();
    }
    
    private void setupButton() {
        setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        setForeground(Color.WHITE);
        setBackground(baseColor);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        
        // Padding interno
        setBorder(new EmptyBorder(12, 24, 12, 24));
    }
    
    private void setupAnimations() {
        // Timer para animação de hover
        hoverTimer = new Timer(15, e -> {
            if (isHovered && hoverProgress < 1f) {
                hoverProgress = Math.min(1f, hoverProgress + 0.1f);
                repaint();
            } else if (!isHovered && hoverProgress > 0f) {
                hoverProgress = Math.max(0f, hoverProgress - 0.1f);
                repaint();
            }
            
            if ((isHovered && hoverProgress >= 1f) || (!isHovered && hoverProgress <= 0f)) {
                hoverTimer.stop();
            }
        });
        
        // Timer para animação de pressão
        pressTimer = new Timer(15, e -> {
            if (isPressed && pressProgress < 1f) {
                pressProgress = Math.min(1f, pressProgress + 0.15f);
                repaint();
            } else if (!isPressed && pressProgress > 0f) {
                pressProgress = Math.max(0f, pressProgress - 0.15f);
                repaint();
            }
            
            if ((isPressed && pressProgress >= 1f) || (!isPressed && pressProgress <= 0f)) {
                pressTimer.stop();
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    hoverTimer.start();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                hoverTimer.start();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    pressTimer.start();
                    startRipple(e.getPoint());
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                pressTimer.start();
            }
        });
    }
    
    private void startRipple(Point point) {
        ripplePoint = point;
        rippleAlpha = 1f;
        
        if (rippleTimer != null && rippleTimer.isRunning()) {
            rippleTimer.stop();
        }
        
        rippleTimer = new Timer(20, e -> {
            rippleAlpha -= 0.05f;
            if (rippleAlpha <= 0f) {
                rippleAlpha = 0f;
                ((Timer)e.getSource()).stop();
            }
            repaint();
        });
        rippleTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int width = getWidth();
        int height = getHeight();
        
        // Desenhar sombra quando hover
        if (hoverProgress > 0) {
            int shadowSize = (int)(8 * hoverProgress);
            g2d.setColor(new Color(0, 0, 0, (int)(30 * hoverProgress)));
            g2d.fillRoundRect(shadowSize/2, shadowSize/2, width - shadowSize, height - shadowSize, 12, 12);
        }
        
        // Calcular cor atual baseada no estado
        Color currentColor = interpolateColor(baseColor, hoverColor, hoverProgress);
        if (pressProgress > 0) {
            currentColor = interpolateColor(currentColor, pressColor, pressProgress);
        }
        
        // Desenhar fundo com gradiente suave
        GradientPaint gradient = new GradientPaint(
        0, 0, darken(currentColor, 0.1f),
        0, height, currentColor
    );

        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, width, height, 12, 12);
        
        // Desenhar brilho superior
        GradientPaint gloss = new GradientPaint(
            0, 0, new Color(255, 255, 255, (int)(40 * (1 - pressProgress))),
            0, height/2, new Color(255, 255, 255, 0)
        );
        g2d.setPaint(gloss);
        g2d.fillRoundRect(0, 0, width, height/2, 12, 12);
        
        // Desenhar efeito ripple
        if (rippleAlpha > 0 && ripplePoint != null) {
            int maxRadius = Math.max(width, height);
            int currentRadius = (int)(maxRadius * (1 - rippleAlpha));
            
            g2d.setColor(new Color(255, 255, 255, (int)(100 * rippleAlpha)));
            g2d.fillOval(
                ripplePoint.x - currentRadius,
                ripplePoint.y - currentRadius,
                currentRadius * 2,
                currentRadius * 2
            );
        }
        
        // Desenhar borda suave
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12);
        
        g2d.dispose();
        
        // Desenhar texto
        super.paintComponent(g);
    }
    
    private Color interpolateColor(Color c1, Color c2, float progress) {
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
        return new Color(r, g, b);
    }
    
    private Color brighten(Color color, float factor) {
        int r = Math.min(255, (int)(color.getRed() + 255 * factor));
        int g = Math.min(255, (int)(color.getGreen() + 255 * factor));
        int b = Math.min(255, (int)(color.getBlue() + 255 * factor));
        return new Color(r, g, b);
    }
    
    private Color darken(Color color, float factor) {
        int r = Math.max(0, (int)(color.getRed() * (1 - factor)));
        int g = Math.max(0, (int)(color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int)(color.getBlue() * (1 - factor)));
        return new Color(r, g, b);
    }
}

/**
 * Botão secundário com estilo outline e animações
 */
class BotaoSecundario extends JButton {
    private float hoverProgress = 0f;
    private Timer hoverTimer;
    private boolean isHovered = false;
    private Color borderColor;
    private Color hoverColor;
    
    // Partículas de fundo
    private java.util.List<Particle> particles = new java.util.ArrayList<>();
    private Timer particleTimer;
    
    public BotaoSecundario(String text, Color borderColor) {
        super(text);
        this.borderColor = borderColor;
        this.hoverColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 30);
        
        setupButton();
        setupAnimations();
    }
    
    private void setupButton() {
        setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        setForeground(new Color(75, 85, 99));
        setBackground(new Color(249, 250, 251));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 20, 12, 20));
    }
    
    private void setupAnimations() {
        hoverTimer = new Timer(15, e -> {
            if (isHovered && hoverProgress < 1f) {
                hoverProgress = Math.min(1f, hoverProgress + 0.08f);
                repaint();
            } else if (!isHovered && hoverProgress > 0f) {
                hoverProgress = Math.max(0f, hoverProgress - 0.08f);
                repaint();
            }
            
            if ((isHovered && hoverProgress >= 1f) || (!isHovered && hoverProgress <= 0f)) {
                hoverTimer.stop();
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    hoverTimer.start();
                    startParticles();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                hoverTimer.start();
                stopParticles();
            }
        });
    }
    
    private void startParticles() {
        particles.clear();
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(getWidth(), getHeight()));
        }
        
        particleTimer = new Timer(30, e -> {
            for (Particle p : particles) {
                p.update();
            }
            repaint();
        });
        particleTimer.start();
    }
    
    private void stopParticles() {
        if (particleTimer != null) {
            particleTimer.stop();
        }
        particles.clear();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Desenhar fundo com transição suave
        Color bgColor = new Color(249, 250, 251);
        if (hoverProgress > 0) {
            int alpha = (int)(30 * hoverProgress);
            g2d.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), alpha));
            g2d.fillRoundRect(0, 0, width, height, 12, 12);
        }
        
        // Desenhar partículas
        for (Particle p : particles) {
            g2d.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), p.alpha));
            g2d.fillOval((int)p.x, (int)p.y, p.size, p.size);
        }
        
        // Desenhar borda com animação
        float borderWidth = 2f + (hoverProgress * 0.5f);
        g2d.setStroke(new BasicStroke(borderWidth));
        
        Color currentBorderColor = interpolateColor(borderColor, borderColor.brighter(), hoverProgress * 0.3f);
        g2d.setColor(currentBorderColor);
        g2d.drawRoundRect(1, 1, width - 3, height - 3, 12, 12);
        
        g2d.dispose();
        super.paintComponent(g);
    }
    
    private Color interpolateColor(Color c1, Color c2, float progress) {
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
        return new Color(r, g, b);
    }
    
    private static class Particle {
        float x, y;
        float vx, vy;
        int size;
        int alpha;
        int maxWidth, maxHeight;
        
        Particle(int maxWidth, int maxHeight) {
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            reset();
        }
        
        void reset() {
            x = (float)(Math.random() * maxWidth);
            y = (float)(Math.random() * maxHeight);
            vx = (float)(Math.random() * 2 - 1);
            vy = (float)(Math.random() * 2 - 1);
            size = (int)(Math.random() * 3 + 2);
            alpha = (int)(Math.random() * 50 + 30);
        }
        
        void update() {
            x += vx;
            y += vy;
            
            if (x < 0 || x > maxWidth || y < 0 || y > maxHeight) {
                reset();
            }
        }
    }
}

/**
 * ComboBox moderno com animações e estilo customizado
 */
class ComboBoxModerno<E> extends JComboBox<E> {
    private float focusProgress = 0f;
    private Timer focusTimer;
    private boolean isFocused = false;
    private Color borderColor = new Color(229, 231, 235);
    private Color focusColor = new Color(99, 102, 241);
    
    public ComboBoxModerno() {
        super();
        setupComboBox();
    }
    
    public ComboBoxModerno(E[] items) {
        super(items);
        setupComboBox();
    }
    
    private void setupComboBox() {
        setFont(new Font("Segoe UI", Font.PLAIN, 14));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        setUI(new ComboBoxModernoUI());
        
        // Renderizador customizado para os itens
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                
                label.setBorder(new EmptyBorder(10, 15, 10, 15));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                
                if (isSelected) {
                    label.setBackground(new Color(99, 102, 241, 40));
                    label.setForeground(new Color(99, 102, 241));
                    label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(new Color(17, 24, 39));
                }
                
                return label;
            }
        });
        
        setupAnimations();
    }
    
    private void setupAnimations() {
        focusTimer = new Timer(15, e -> {
            if (isFocused && focusProgress < 1f) {
                focusProgress = Math.min(1f, focusProgress + 0.1f);
                repaint();
            } else if (!isFocused && focusProgress > 0f) {
                focusProgress = Math.max(0f, focusProgress - 0.1f);
                repaint();
            }
            
            if ((isFocused && focusProgress >= 1f) || (!isFocused && focusProgress <= 0f)) {
                focusTimer.stop();
            }
        });
        
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                focusTimer.start();
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                focusTimer.start();
            }
        });
        
        addMouseListener(new MouseAdapter() {
            private boolean wasPressed = false;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled() && !wasPressed) {
                    isFocused = true;
                    focusTimer.start();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!hasFocus() && !wasPressed) {
                    isFocused = false;
                    focusTimer.start();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                wasPressed = true;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                wasPressed = false;
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Fundo branco
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(0, 0, width, height, 10, 10);
        
        // Borda animada
        Color currentBorderColor = interpolateColor(borderColor, focusColor, focusProgress);
        float borderWidth = 2f + (focusProgress * 0.5f);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.setColor(currentBorderColor);
        g2d.drawRoundRect(1, 1, width - 3, height - 3, 10, 10);
        
        // Brilho quando focado
        if (focusProgress > 0) {
            g2d.setColor(new Color(99, 102, 241, (int)(20 * focusProgress)));
            g2d.fillRoundRect(0, 0, width, height, 10, 10);
        }
        
        g2d.dispose();
        super.paintComponent(g);
    }
    
    private Color interpolateColor(Color c1, Color c2, float progress) {
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
        return new Color(r, g, b);
    }
    
    public float getFocusProgress() {
        return focusProgress;
    }
}

/**
 * UI customizada para o ComboBox
 */
class ComboBoxModernoUI extends BasicComboBoxUI {
    
    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton() {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // Desenhar seta
                int arrowSize = 8;
                int centerX = width / 2;
                int centerY = height / 2;
                
                Polygon arrow = new Polygon();
                arrow.addPoint(centerX - arrowSize / 2, centerY - 2);
                arrow.addPoint(centerX + arrowSize / 2, centerY - 2);
                arrow.addPoint(centerX, centerY + arrowSize / 2 - 2);
                
                g2d.setColor(new Color(107, 114, 128));
                g2d.fillPolygon(arrow);
                
                g2d.dispose();
            }
        };
        
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(30, 30));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox) {
            @Override
            protected void configurePopup() {
                super.configurePopup();
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                    BorderFactory.createEmptyBorder(5, 0, 5, 0)
                ));
            }
            
            @Override
            protected void configureList() {
                super.configureList();
                list.setBackground(Color.WHITE);
            }
        };
        return popup;
    }
}

/**
 * Classe de exemplo demonstrando o uso dos componentes
 */
class ExemploComponentes extends JFrame {
    public ExemploComponentes() {
        super("Componentes Modernos e Animados");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        panel.setBackground(new Color(249, 250, 251));
        
        // Título
        JLabel titulo = new JLabel("🎨 Componentes UI Modernos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setForeground(new Color(17, 24, 39));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titulo);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        // Botões primários
        JLabel labelPrimarios = new JLabel("Botões Primários:");
        labelPrimarios.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelPrimarios.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelPrimarios);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        //BotaoModerno btn1 = new BotaoModerno("🚀 Calcular Rota", new Color(99, 102, 241));
        BotaoModerno btn1 = new BotaoModerno("🚀 Calcular Rota", new Color(99, 102, 241));
        btn1.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn1.setMaximumSize(new Dimension(300, 50));
        panel.add(btn1);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        BotaoModerno btn2 = new BotaoModerno("✅ Executar Algoritmo", new Color(34, 197, 94));
        btn2.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn2.setMaximumSize(new Dimension(300, 50));
        panel.add(btn2);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        BotaoModerno btn3 = new BotaoModerno("❌ Cancelar Operação", new Color(239, 68, 68));
        btn3.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn3.setMaximumSize(new Dimension(300, 50));
        panel.add(btn3);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        // Botões secundários
        JLabel labelSecundarios = new JLabel("Botões Secundários:");
        labelSecundarios.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelSecundarios.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelSecundarios);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        BotaoSecundario btn4 = new BotaoSecundario("📊 Matriz de Adjacência", new Color(99, 102, 241));
        btn4.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn4.setMaximumSize(new Dimension(300, 50));
        panel.add(btn4);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        BotaoSecundario btn5 = new BotaoSecundario("🌳 Algoritmo de Kruskal", new Color(139, 92, 246));
        btn5.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn5.setMaximumSize(new Dimension(300, 50));
        panel.add(btn5);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        // ComboBoxes
        JLabel labelCombo = new JLabel("ComboBoxes Animados:");
        labelCombo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelCombo);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        String[] cidades = {"Kiev", "Lviv", "Odessa", "Kharkiv", "Dnipro"};
        ComboBoxModerno<String> combo1 = new ComboBoxModerno<>(cidades);
        combo1.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo1.setMaximumSize(new Dimension(300, 45));
        panel.add(combo1);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        ComboBoxModerno<String> combo2 = new ComboBoxModerno<>(cidades);
        combo2.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo2.setMaximumSize(new Dimension(300, 45));
        panel.add(combo2);
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane);
        setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ExemploComponentes();
        });
    }
}