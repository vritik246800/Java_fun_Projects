import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PainelDataComCalendarioDeluxe extends JPanel {

    private final JSpinner spinner;
    private final JButton btnCalendario;
    private final JButton btnConfirmar;
    private boolean focoAtivo = false;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public PainelDataComCalendarioDeluxe() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        setPreferredSize(new Dimension(460, 130));
        setBackground(new Color(245, 245, 245));

        JLabel label = new JLabel("📅 Data e Hora:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));

        SpinnerDateModel model = new SpinnerDateModel();
        spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy HH:mm"));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        spinner.setPreferredSize(new Dimension(170, 35));

        spinner.getEditor().getComponent(0).addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focoAtivo = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focoAtivo = false;
                repaint();
            }
        });

        btnCalendario = criarBotaoAnimado("[17]", new Color(230, 230, 230), e -> abrirCalendarioPopup());
        btnConfirmar = criarBotaoAnimado("Confirmar", new Color(33, 150, 243), e -> {
            Date data = (Date) spinner.getValue();
            JOptionPane.showMessageDialog(this, "Selecionou: " + sdf.format(data));
        });

        add(label);
        add(spinner);
        add(btnCalendario);
        add(btnConfirmar);
    }

    // Botão com gradiente, bordas arredondadas e hover animado
    private JButton criarBotaoAnimado(String texto, Color corBase, ActionListener acao) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint grad = new GradientPaint(0, 0, corBase.brighter(), 0, getHeight(), corBase.darker());
                g2.setPaint(grad);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                // desenhar texto centralizado
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(getText());
                int th = fm.getAscent();
                g2.drawString(getText(), (getWidth() - tw) / 2, (getHeight() + th / 2) / 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 35));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(acao);

        // Hover animado (simples)
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(corBase.brighter());
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(corBase);
                btn.repaint();
            }
        });
        return btn;
    }

    // Cria botão pequeno com cantos curvos
    private JButton criarBotaoCurvo(String texto) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setBackground(new Color(25, 118, 210));
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void abrirCalendarioPopup() {
        final JDialog dialog = new JDialog((Frame) null, "Selecionar Data e Hora", true);
        dialog.setUndecorated(true);
        dialog.setSize(340, 420);
        dialog.setLayout(new BorderLayout());
        dialog.setOpacity(0f);

        // Painel principal com borda arredondada (estética)
        JPanel painelPrincipal = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
            }
        };
        painelPrincipal.setBackground(Color.WHITE);
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(painelPrincipal);

        final Calendar cal = Calendar.getInstance();
        cal.setTime((Date) spinner.getValue());

        // Cabeçalho com mês e ano
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(new Color(33, 150, 243));
        cabecalho.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JButton btnAnterior = criarBotaoCurvo("<");
        JButton btnProximo = criarBotaoCurvo(">");

        String[] meses = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        JComboBox<String> comboMes = new JComboBox<>(meses);
        comboMes.setSelectedIndex(cal.get(Calendar.MONTH));

        Integer[] anos = new Integer[201];
        for (int i = 0; i < anos.length; i++) anos[i] = 1900 + i;
        JComboBox<Integer> comboAno = new JComboBox<>(anos);
        comboAno.setSelectedItem(cal.get(Calendar.YEAR));

        JPanel painelSelecao = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        painelSelecao.setOpaque(false);
        painelSelecao.add(comboMes);
        painelSelecao.add(comboAno);

        cabecalho.add(btnAnterior, BorderLayout.WEST);
        cabecalho.add(painelSelecao, BorderLayout.CENTER);
        cabecalho.add(btnProximo, BorderLayout.EAST);

        JPanel calendario = new JPanel(new GridLayout(0, 7));
        calendario.setBackground(Color.WHITE);

        // Painel inferior para hora/minuto e botões
        JPanel painelInferior = new JPanel(new BorderLayout());
        painelInferior.setBackground(Color.WHITE);
        JPanel painelHora = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        painelHora.setBackground(Color.WHITE);

        SpinnerNumberModel horaModel = new SpinnerNumberModel(cal.get(Calendar.HOUR_OF_DAY), 0, 23, 1);
        SpinnerNumberModel minutoModel = new SpinnerNumberModel(cal.get(Calendar.MINUTE), 0, 59, 1);
        final JSpinner spinnerHora = new JSpinner(horaModel);
        final JSpinner spinnerMinuto = new JSpinner(minutoModel);

        painelHora.add(new JLabel("Hora:"));
        painelHora.add(spinnerHora);
        painelHora.add(new JLabel("Minuto:"));
        painelHora.add(spinnerMinuto);

        // Botões de ação na parte inferior
        JPanel botoesInferiores = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botoesInferiores.setOpaque(false);
        JButton btnHoje = criarBotaoAnimado("Hoje", new Color(76, 175, 80), ev -> {
            Calendar hoje = Calendar.getInstance();
            spinnerHora.setValue(hoje.get(Calendar.HOUR_OF_DAY));
            spinnerMinuto.setValue(hoje.get(Calendar.MINUTE));
            comboMes.setSelectedIndex(hoje.get(Calendar.MONTH));
            comboAno.setSelectedItem(hoje.get(Calendar.YEAR));
            // atualizar calendário vai selecionar o dia depois
        });
        JButton btnOk = criarBotaoAnimado("OK", new Color(33, 150, 243), ev -> {
            // aplica seleção atual no cal e fecha
            cal.set(Calendar.HOUR_OF_DAY, (Integer) spinnerHora.getValue());
            cal.set(Calendar.MINUTE, (Integer) spinnerMinuto.getValue());
            spinner.setValue(cal.getTime());
            // fade-out + dispose usando Timer
            Timer t = new Timer(12, null);
            final int[] step = {20};
            t.addActionListener(ae -> {
                step[0]--;
                float op = Math.max(0f, step[0] / 20f);
                dialog.setOpacity(op);
                if (step[0] <= 0) {
                    t.stop();
                    dialog.dispose();
                }
            });
            t.start();
        });
        botoesInferiores.add(btnHoje);
        botoesInferiores.add(btnOk);

        painelInferior.add(painelHora, BorderLayout.CENTER);
        painelInferior.add(botoesInferiores, BorderLayout.SOUTH);

        // Função para preencher o calendário (recria botões)
        Runnable atualizarCalendario = () -> {
            calendario.removeAll();
            String[] dias = {"D", "S", "T", "Q", "Q", "S", "S"};
            for (String d : dias) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setForeground(Color.GRAY);
                calendario.add(lbl);
            }

            int mes = comboMes.getSelectedIndex();
            int ano = (int) comboAno.getSelectedItem();
            Calendar tempCal = new GregorianCalendar(ano, mes, 1);
            int primeiroDiaSemana = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
            int diasNoMes = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < primeiroDiaSemana; i++) calendario.add(new JLabel(""));

            for (int i = 1; i <= diasNoMes; i++) {
                final int diaNum = i; // <--- torna 'diaNum' efetivamente final para a lambda
                JButton diaBtn = new JButton(String.valueOf(diaNum));
                diaBtn.setFocusPainted(false);
                diaBtn.setBackground(Color.WHITE);
                diaBtn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

                // destaca o dia atual no calendário, se pertinente
                if (diaNum == cal.get(Calendar.DAY_OF_MONTH)
                        && mes == cal.get(Calendar.MONTH)
                        && ano == cal.get(Calendar.YEAR)) {
                    diaBtn.setBackground(new Color(33, 150, 243));
                    diaBtn.setForeground(Color.WHITE);
                }

                diaBtn.addActionListener(e -> {
                    // aplica dia/mes/ano e hora/minuto selecionados
                    cal.set(Calendar.DAY_OF_MONTH, diaNum);
                    cal.set(Calendar.MONTH, mes);
                    cal.set(Calendar.YEAR, ano);
                    cal.set(Calendar.HOUR_OF_DAY, (Integer) spinnerHora.getValue());
                    cal.set(Calendar.MINUTE, (Integer) spinnerMinuto.getValue());
                    spinner.setValue(cal.getTime());
                    // animação de fechar (fade out)
                    Timer t = new Timer(12, null);
                    final int[] step = {20};
                    t.addActionListener(ae -> {
                        step[0]--;
                        float op = Math.max(0f, step[0] / 20f);
                        dialog.setOpacity(op);
                        if (step[0] <= 0) {
                            t.stop();
                            dialog.dispose();
                        }
                    });
                    t.start();
                });
                calendario.add(diaBtn);
            }

            calendario.revalidate();
            calendario.repaint();
        };

        // listeners de navegação
        comboMes.addActionListener(e -> atualizarCalendario.run());
        comboAno.addActionListener(e -> atualizarCalendario.run());

        btnAnterior.addActionListener(e -> {
            int m = comboMes.getSelectedIndex() - 1;
            if (m < 0) {
                comboMes.setSelectedIndex(11);
                comboAno.setSelectedItem((Integer) comboAno.getSelectedItem() - 1);
            } else comboMes.setSelectedIndex(m);
        });
        btnProximo.addActionListener(e -> {
            int m = comboMes.getSelectedIndex() + 1;
            if (m > 11) {
                comboMes.setSelectedIndex(0);
                comboAno.setSelectedItem((Integer) comboAno.getSelectedItem() + 1);
            } else comboMes.setSelectedIndex(m);
        });

        atualizarCalendario.run();

        painelPrincipal.add(cabecalho, BorderLayout.NORTH);
        painelPrincipal.add(calendario, BorderLayout.CENTER);
        painelPrincipal.add(painelInferior, BorderLayout.SOUTH);

        // Posição inicial (embaixo do componente)
        Point origem;
        try {
            origem = getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            origem = new Point(100, 100);
        }
        dialog.setLocation(origem.x, origem.y + getHeight() + 6);

        // Animação slide + fade in (usando Timer no EDT)
        final Point origemFinal = origem; // <- variável final para usar na lambda
        Timer animator = new Timer(15, null);
        final int[] step = {0};
        final int stepsTotal = 16;
        final int startY = origemFinal.y + getHeight() + 60;
        final int endY = origemFinal.y + getHeight() + 6;
        dialog.setLocation(origemFinal.x, startY);
        dialog.setOpacity(0f);

        animator.addActionListener(ae -> {
            step[0]++;
            float t = Math.min(1f, step[0] / (float) stepsTotal);
            int y = startY - Math.round((startY - endY) * t);
            dialog.setLocation(origemFinal.x, y);
            dialog.setOpacity(Math.min(1f, t));
            if (step[0] >= stepsTotal) animator.stop();
        });
        animator.start();

        // Fecha se perder foco
        dialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                // fade out e dispose
                Timer t = new Timer(12, null);
                final int[] s = {20};
                t.addActionListener(ae -> {
                    s[0]--;
                    float op = Math.max(0f, s[0] / 20f);
                    dialog.setOpacity(op);
                    if (s[0] <= 0) {
                        t.stop();
                        dialog.dispose();
                    }
                });
                t.start();
            }
        });

        dialog.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (focoAtivo) {
            int x = spinner.getX() - 5;
            int y = spinner.getY() - 3;
            int w = spinner.getWidth() + 10;
            int h = spinner.getHeight() + 6;
            g2.setColor(new Color(33, 150, 243, 80));
            g2.fillRoundRect(x, y, w, h, 10, 10);
        }
    }

    // Teste
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Painel de Data e Hora Deluxe ✨");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(new PainelDataComCalendarioDeluxe(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
