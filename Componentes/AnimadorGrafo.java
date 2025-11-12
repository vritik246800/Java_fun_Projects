import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import models.Aresta;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class AnimadorGrafo implements ActionListener {
    private Timer timer;
    private List<Aresta> passos;
    private PainelGrafo painelGrafo;
    private int indiceAtual;
    private boolean emExecucao;
    private JButton btnIniciar;
    private JButton btnParar;
    private JLabel labelProgresso;
    private Runnable aoFinalizar;
    
    // Velocidade da animação (ms entre passos)
    private static final int VELOCIDADE_PADRAO = 800;
    
    public AnimadorGrafo(PainelGrafo painelGrafo, JButton btnIniciar, JButton btnParar, JLabel labelProgresso) {
        this.painelGrafo = painelGrafo;
        this.btnIniciar = btnIniciar;
        this.btnParar = btnParar;
        this.labelProgresso = labelProgresso;
        this.timer = new Timer(VELOCIDADE_PADRAO, this);
        this.indiceAtual = 0;
        this.emExecucao = false;
    }
    
    public void iniciarAnimacao(List<Aresta> passos, Runnable aoFinalizar) {
        if (passos == null || passos.isEmpty()) {
            JOptionPane.showMessageDialog(null, 
                "Não há passos para animar!", 
                "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        this.passos = passos;
        this.aoFinalizar = aoFinalizar;
        this.indiceAtual = 0;
        this.emExecucao = true;
        
        btnIniciar.setEnabled(false);
        btnParar.setEnabled(true);
        
        atualizarProgresso();
        timer.start();
    }
    
    public void pararAnimacao() {
        timer.stop();
        emExecucao = false;
        
        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);
        
        // Mostrar resultado final - criar nova lista
        if (passos != null && indiceAtual > 0) {
            List<Aresta> caminhoAtual = new ArrayList<>(passos.subList(0, Math.min(indiceAtual, passos.size())));
            painelGrafo.setCaminhoDestacado(caminhoAtual);
            labelProgresso.setText("⏸️ Animação pausada - " + indiceAtual + "/" + passos.size() + " passos");
        }
    }
    
    public void resetar() {
        timer.stop();
        emExecucao = false;
        indiceAtual = 0;
        passos = null;
        
        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);
        
        painelGrafo.setCaminhoDestacado(null);
        labelProgresso.setText("⏳ Aguardando execução...");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!emExecucao || passos == null) {
            return;
        }
        
        if (indiceAtual < passos.size()) {
            // Mostrar apenas os passos até o índice atual - criar nova lista
            List<Aresta> passosAtuais = new ArrayList<>(passos.subList(0, indiceAtual + 1));
            painelGrafo.setCaminhoDestacado(passosAtuais);
            
            indiceAtual++;
            atualizarProgresso();
        } else {
            // Animação concluída
            finalizarAnimacao();
        }
    }
    
    private void atualizarProgresso() {
        if (passos != null && labelProgresso != null) {
            int total = passos.size();
            int atual = Math.min(indiceAtual, total);
            int percentagem = (int) ((atual / (double) total) * 100);
            
            labelProgresso.setText(String.format("▶️ Progresso: %d/%d passos (%d%%)", 
                atual, total, percentagem));
        }
    }
    
    private void finalizarAnimacao() {
        timer.stop();
        emExecucao = false;
        
        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);
        
        labelProgresso.setText("✅ Animação concluída - " + passos.size() + " passos executados");
        
        // Executar callback de finalização
        if (aoFinalizar != null) {
            aoFinalizar.run();
        }
    }
    
    public boolean estaEmExecucao() {
        return emExecucao;
    }
    
    public void setVelocidade(int milissegundos) {
        timer.setDelay(milissegundos);
    }
}