import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import models.No;
import models.Aresta;
import models.Grafo;
import algoritms.TSP;
import algoritms.ResultadoTSP;
import util.ConversorTempo;

public class AnimadorComboio {
    private PainelGrafo painelGrafo;
    private JButton btnIniciar;
    private JButton btnParar;
    private JButton btnContinuar; // Novo botão para continuar
    private JLabel labelProgresso;
    private Timer timer;
    private Image imagemComboio;
    private TSP tsp;

    private List<No> rotaCompleta;
    private List<Aresta> arestasRota;
    private int indiceCaminhoAtual = 0;
    private double progresso = 0.0;
    private boolean animando = false;
    private boolean pausado = false; // Estado de pausa

    private Point2D.Double posicaoAtual;
    private double anguloRotacao = 0;

    private static final int VELOCIDADE = 15;
    private static final double INCREMENTO = 0.01;

    // ✅ Callback para integração com janela de monitorização
    private java.util.function.BiConsumer<Integer, String> onProgressUpdate;

    public void setOnProgressUpdate(java.util.function.BiConsumer<Integer, String> callback) {
        this.onProgressUpdate = callback;
    }

    public AnimadorComboio(PainelGrafo painelGrafo, JButton btnIniciar, JButton btnParar, JLabel labelProgresso) {
        this.painelGrafo = painelGrafo;
        this.btnIniciar = btnIniciar;
        this.btnParar = btnParar;
        this.labelProgresso = labelProgresso;
        this.tsp = new TSP();
        carregarImagemComboio();
    }

    // Construtor alternativo com botão continuar
    public AnimadorComboio(PainelGrafo painelGrafo, JButton btnIniciar, JButton btnParar, JButton btnContinuar, JLabel labelProgresso) {
        this(painelGrafo, btnIniciar, btnParar, labelProgresso);
        this.btnContinuar = btnContinuar;
    }

    // Setter para adicionar botão pausar depois
    public void setBotaoPausar(JButton btnPausar) {
        this.btnContinuar = btnPausar;
    }

    // 🔹 Carregar imagem do comboio (fallback visual se faltar o arquivo)
    private void carregarImagemComboio() {
        try {
            imagemComboio = new ImageIcon("Image/train2.gif").getImage();
            if (imagemComboio.getWidth(null) <= 0) {
                System.err.println("⚠️ Aviso: train.gif não encontrado. Usando representação alternativa.");
                imagemComboio = null;
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar train.gif: " + e.getMessage());
            imagemComboio = null;
        }
    }

    // 🔹 Iniciar percurso completo usando TSP
    public void iniciarPercursoCompleto(No origem, Grafo grafo) {
        if (animando && !pausado) pararAnimacao();

        // Usar TSP para calcular a rota
        ResultadoTSP resultado = tsp.vizinhoMaisProximo(grafo, origem);

        if (resultado == null || resultado.getRota().size() < 2) {
            labelProgresso.setText("❌ Rota inválida!");
            JOptionPane.showMessageDialog(null,
                "Não foi possível criar uma rota que visite todos os pontos.",
                "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        rotaCompleta = resultado.getRota();
        arestasRota = resultado.getArestasPercorridas();

        if (arestasRota == null || arestasRota.isEmpty()) {
            labelProgresso.setText("❌ Nenhuma aresta na rota!");
            JOptionPane.showMessageDialog(null,
                "Erro ao gerar arestas da rota TSP.",
                "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        painelGrafo.setCaminhoDestacado(arestasRota);
        indiceCaminhoAtual = 0;
        progresso = 0.0;
        pausado = false;

        posicaoAtual = new Point2D.Double(
            rotaCompleta.get(0).getX(),
            rotaCompleta.get(0).getY()
        );

        animando = true;
        btnIniciar.setEnabled(false);
        btnParar.setEnabled(true);
        if (btnContinuar != null) btnContinuar.setEnabled(false);

        String status = resultado.isRotaCompleta() ?
            "🚂 Iniciando percurso completo (circular)..." :
            "🚂 Iniciando percurso (não circular)...";
        labelProgresso.setText(status);

        iniciarTimer();
    }

    // 🔹 Método legado para compatibilidade (converte para usar Grafo)
    @Deprecated
    public void iniciarPercursoCompleto(No origem, List<No> todasCidades, List<Aresta> todasConexoes) {
        // Criar Grafo temporário para usar com TSP
        Grafo grafoTemp = new Grafo();
        for (No no : todasCidades) {
            grafoTemp.addNo(no);
        }
        // Usar Set para evitar duplicatas de arestas
        Set<String> arestasAdicionadas = new HashSet<>();
        for (Aresta aresta : todasConexoes) {
            String chave = aresta.getOrigem().getNome() + "-" + aresta.getDestino().getNome();
            if (!arestasAdicionadas.contains(chave)) {
                grafoTemp.addAresta(aresta.getOrigem().getNome(), aresta.getDestino().getNome(), aresta.getPeso());
                arestasAdicionadas.add(chave);
            }
        }
        iniciarPercursoCompleto(origem, grafoTemp);
    }

    public void resetar() {
        pararAnimacao(); // garante que a animação para
        indiceCaminhoAtual = 0;
        progresso = 0.0;
        posicaoAtual = null;
        anguloRotacao = 0.0;
        pausado = false;
        painelGrafo.repaint();
    }

    // 🔹 Pausar animação (mantém estado)
    public void pausarAnimacao() {
        if (!animando) return;

        if (timer != null) timer.stop();
        pausado = true;
        animando = false;

        btnIniciar.setEnabled(false);
        btnParar.setEnabled(false);
        if (btnContinuar != null) btnContinuar.setEnabled(true);

        labelProgresso.setText("⏸️ Animação pausada");
    }

    // 🔹 Continuar animação de onde parou
    public void continuarAnimacao() {
        if (!pausado || arestasRota == null || arestasRota.isEmpty()) return;

        pausado = false;
        animando = true;

        btnIniciar.setEnabled(false);
        btnParar.setEnabled(true);
        if (btnContinuar != null) btnContinuar.setEnabled(false);

        labelProgresso.setText("🚂 Continuando percurso...");
        iniciarTimer();
    }

    // 🚂 Atualiza o movimento do comboio e notifica o progresso
    private void atualizarAnimacao() {
        if (!animando || arestasRota.isEmpty()) return;

        progresso += INCREMENTO;
        if (progresso > 1.0) progresso = 1.0;

        if (progresso >= 1.0) {
            progresso = 0.0;
            indiceCaminhoAtual++;
            if (indiceCaminhoAtual >= arestasRota.size()) {
                finalizarAnimacao();
                return;
            }
        }

        Aresta arestaAtual = arestasRota.get(indiceCaminhoAtual);
        No origem = arestaAtual.getOrigem();
        No destino = arestaAtual.getDestino();

        // Calcular posição usando curva de Bézier quadrática (mesma lógica do PainelGrafo)
        posicaoAtual = calcularPontoNaCurva(origem, destino, progresso);

        // Calcular ângulo de rotação baseado na tangente da curva
        // Para suavidade, calculamos um ponto ligeiramente à frente
        double deltaT = 0.01;
        Point2D.Double pontoFrente = calcularPontoNaCurva(origem, destino, Math.min(progresso + deltaT, 1.0));
        anguloRotacao = Math.atan2(pontoFrente.getY() - posicaoAtual.getY(), pontoFrente.getX() - posicaoAtual.getX());

        int totalSegmentos = arestasRota.size();
        int segmentoAtual = indiceCaminhoAtual + 1;
        int percentagem = (int) ((segmentoAtual * 100.0) / totalSegmentos);

        labelProgresso.setText(String.format(
            "🚂 Percurso: %d/%d segmentos (%.0f%%)",
            segmentoAtual, totalSegmentos, (double) percentagem
        ));

        // ✅ Atualiza painel de informações no mapa
        painelGrafo.atualizarInfoComboio(
            origem.getNome(),
            destino.getNome(),
            arestaAtual.getPeso(),
            percentagem
        );

        // ✅ Notifica a janela de monitorização (callback)
        if (onProgressUpdate != null) {
            onProgressUpdate.accept(percentagem, labelProgresso.getText());
        }

        painelGrafo.repaint();
    }

    private void iniciarTimer() {
        timer = new Timer(VELOCIDADE, e -> atualizarAnimacao());
        timer.start();
    }

    private void finalizarAnimacao() {
        animando = false;
        if (timer != null) timer.stop();
        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);
        labelProgresso.setText("✅ Percurso completo concluído!");

        // Esconde o painel de informações
        painelGrafo.esconderInfoComboio();
        painelGrafo.repaint();

        // Calcular distância total e tempo
        int distanciaTotal = 0;
        if (arestasRota != null) {
            for (Aresta aresta : arestasRota) {
                distanciaTotal += aresta.getPeso();
            }
        }

        // Mostrar JOptionPane personalizado com resumo
        mostrarResumoPercurso(distanciaTotal);

        // Última atualização na janela externa
        if (onProgressUpdate != null) {
            onProgressUpdate.accept(100, "✅ Percurso completo concluído!");
        }
    }

    public void pararAnimacao() {
        if (timer != null) timer.stop();
        animando = false;
        pausado = false; // Reset completo

        // Reset do estado
        indiceCaminhoAtual = 0;
        progresso = 0.0;

        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);
        if (btnContinuar != null) btnContinuar.setEnabled(false);

        labelProgresso.setText("⏹️ Animação parada");

        // Esconde o painel de informações
        painelGrafo.esconderInfoComboio();
        painelGrafo.repaint();
    }

    public boolean isAnimando() {
        return animando;
    }

    public void desenharComboio(Graphics2D g2d) {
        if (!animando || posicaoAtual == null) return;
        Graphics2D g = (Graphics2D) g2d.create();
        g.translate(posicaoAtual.x, posicaoAtual.y);
        g.rotate(anguloRotacao);

        if (imagemComboio != null) {
            int largura = 40;
            int altura = 30;
            g.drawImage(imagemComboio, -largura / 2, -altura / 2, largura, altura, null);
        } else {
            g.setColor(new Color(220, 50, 50));
            g.fillRect(-20, -12, 40, 24);
            g.setColor(Color.WHITE);
            g.fillRect(-8, -8, 6, 6);
            g.fillRect(2, -8, 6, 6);
        }
        g.dispose();
    }

    /**
     * Calcula um ponto na curva de Bézier quadrática.
     * Usa a mesma lógica do PainelGrafo para garantir que o comboio siga as curvas desenhadas.
     *
     * Fórmula de Bézier quadrática:
     * P(t) = (1-t)²·P0 + 2(1-t)t·Pc + t²·P1
     *
     * @param origem Ponto de origem (P0)
     * @param destino Ponto de destino (P1)
     * @param t Parâmetro da curva (0 a 1)
     * @return Ponto calculado na curva
     */
    private Point2D.Double calcularPontoNaCurva(No origem, No destino, double t) {
        // Coordenadas de origem e destino
        double x0 = origem.getX();
        double y0 = origem.getY();
        double x2 = destino.getX();
        double y2 = destino.getY();

        // Ponto de controle (mesma lógica do PainelGrafo.criarQuad)
        double cx = (x0 + x2) / 2.0;  // offsetX = 0
        double cy = (y0 + y2) / 2.0 - 60;  // control point acima para arco

        // Fórmula de Bézier quadrática
        double x = (1 - t) * (1 - t) * x0 + 2 * (1 - t) * t * cx + t * t * x2;
        double y = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * cy + t * t * y2;

        return new Point2D.Double(x, y);
    }

    /**
     * Mostra um JOptionPane personalizado com o resumo do percurso
     */
    private void mostrarResumoPercurso(int distanciaTotal) {
        String tempoFormatado = ConversorTempo.pesoParaTempo(distanciaTotal);
        int numeroCidades = rotaCompleta != null ? rotaCompleta.size() : 0;
        int numeroSegmentos = arestasRota != null ? arestasRota.size() : 0;

        // Criar painel personalizado
        JPanel painelResumo = new JPanel();
        painelResumo.setLayout(new BoxLayout(painelResumo, BoxLayout.Y_AXIS));
        painelResumo.setBackground(new Color(240, 248, 255));
        painelResumo.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Título
        JLabel labelTitulo = new JLabel("🎉 Percurso Concluído!");
        labelTitulo.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        labelTitulo.setForeground(new Color(30, 90, 140));
        labelTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        painelResumo.add(labelTitulo);

        painelResumo.add(Box.createRigidArea(new Dimension(0, 15)));

        // Linha separadora
        JSeparator separador = new JSeparator();
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        painelResumo.add(separador);

        painelResumo.add(Box.createRigidArea(new Dimension(0, 15)));

        // Informações
        adicionarInfoLinha(painelResumo, "📍 Cidades visitadas:", String.valueOf(numeroCidades));
        adicionarInfoLinha(painelResumo, "🔗 Segmentos percorridos:", String.valueOf(numeroSegmentos));
        adicionarInfoLinha(painelResumo, "📏 Distância total:", distanciaTotal + " km");
        adicionarInfoLinha(painelResumo, "⏱️ Tempo total:", tempoFormatado);

        // Mostrar dialog personalizado
        JOptionPane.showMessageDialog(
            painelGrafo,
            painelResumo,
            "Resumo do Percurso",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Adiciona uma linha de informação ao painel
     */
    private void adicionarInfoLinha(JPanel painel, String label, String valor) {
        JPanel linhaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        linhaPanel.setBackground(new Color(240, 248, 255));
        linhaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelTexto = new JLabel(label);
        labelTexto.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        labelTexto.setForeground(new Color(60, 60, 60));

        JLabel labelValor = new JLabel("  " + valor);
        labelValor.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        labelValor.setForeground(new Color(30, 90, 140));

        linhaPanel.add(labelTexto);
        linhaPanel.add(labelValor);

        painel.add(linhaPanel);
    }
}
