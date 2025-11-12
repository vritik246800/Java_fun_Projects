import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SwitchVelocidadeAnimado extends JPanel {

    private int estadoAtual = 0;       // Estado ativo (0–3)
    private float posAnimada = 0f;     // Posição animada (0.0–3.0)
    private Color corAtual;            // Cor intermediária animada
    private final int estados = 4;

    // Textos e cores
    private final String[] textos = {"LENTO", "MÉDIO", "RÁPIDO", "TURBO"};
    private final Color[] cores = {
        Color.GREEN,
        Color.YELLOW,
        Color.ORANGE,
        Color.RED
    };

    // Tamanho fixo
    private final int largura = 250;
    private final int altura = 50;

    // Animação
    private Timer animador;
    private final int duracao = 300; // milissegundos
    private final int frames = 60;

    public SwitchVelocidadeAnimado() {
        setPreferredSize(new Dimension(largura, altura));
        corAtual = cores[0];

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int proximo = (estadoAtual + 1) % estados;
                animarMudanca(proximo);
            }
        });
    }

    private void animarMudanca(int novoEstado) {
        if (animador != null && animador.isRunning()) animador.stop();

        float inicioPos = posAnimada;
        float destinoPos = novoEstado;

        Color inicioCor = corAtual;
        Color destinoCor = cores[novoEstado];

        float incremento = (destinoPos - inicioPos) / frames;

        animador = new Timer(duracao / frames, null);
        final int[] frame = {0};

        animador.addActionListener(e -> {
            frame[0]++;
            posAnimada += incremento;

            // Interpolar cor (fade)
            float t = (float) frame[0] / frames;
            int r = (int) (inicioCor.getRed() + t * (destinoCor.getRed() - inicioCor.getRed()));
            int g = (int) (inicioCor.getGreen() + t * (destinoCor.getGreen() - inicioCor.getGreen()));
            int b = (int) (inicioCor.getBlue() + t * (destinoCor.getBlue() - inicioCor.getBlue()));
            corAtual = new Color(r, g, b);

            repaint();

            if (frame[0] >= frames) {
                estadoAtual = novoEstado;
                posAnimada = destinoPos;
                corAtual = destinoCor;
                animador.stop();
            }
        });

        animador.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = altura;
        int margem = 6;
        int diametro = altura - 2 * margem;

        // Fundo animado (cor em transição)
        g2.setColor(corAtual);
        g2.fillRoundRect(0, 0, largura, altura, arc, arc);

        // Círculo deslizante
        int maxDeslocamento = largura - diametro - 2 * margem;
        int posX = margem + (int) ((posAnimada / (estados - 1)) * maxDeslocamento);
        int posY = margem;

        g2.setColor(Color.WHITE);
        g2.fillOval(posX, posY, diametro, diametro);

        // Texto
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        String texto = textos[estadoAtual];
        int textWidth = fm.stringWidth(texto);
        int textHeight = fm.getAscent();

        g2.setColor(Color.WHITE);
        g2.drawString(texto, (largura - textWidth) / 2, (altura + textHeight / 2) / 2);
    }

    public String getEstado() {
        return textos[estadoAtual];
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Switch de Velocidade (Animado)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());
        frame.add(new SwitchVelocidadeAnimado());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
