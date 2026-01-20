package ui;

import network.FileReceiver;
import network.FileSender;
import object.Device;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class MainWindow extends JFrame {

    // Cores do tema dark
    private static final Color BG_PRIMARY = new Color(18, 18, 18);
    private static final Color BG_SECONDARY = new Color(28, 28, 28);
    private static final Color BG_CARD = new Color(38, 38, 38);
    private static final Color ACCENT = new Color(99, 102, 241); // Indigo
    private static final Color ACCENT_HOVER = new Color(129, 140, 248);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 240);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 160);
    private static final Color SUCCESS = new Color(34, 197, 94);
    private static final Color ERROR = new Color(239, 68, 68);

    private JTextArea log;
    private File selected;
    private JProgressBar progress;
    private DefaultListModel<Device> deviceModel;
    private JList<Device> deviceList;
    private FileReceiver fileReceiver;
    private JLabel selectedFileLabel;
    private JButton sendBtn;

    public MainWindow() {
        setTitle("Transferência LAN");
        setSize(1000, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Dark theme
        getContentPane().setBackground(BG_PRIMARY);
        
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(BG_PRIMARY);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // === HEADER ===
        JPanel header = createHeader();
        
        // === CONTEÚDO CENTRAL ===
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setBackground(BG_PRIMARY);
        
        // Painel esquerdo - Dispositivos
        JPanel devicesPanel = createDevicesPanel();
        
        // Painel direito - Ações e Log
        JPanel actionsPanel = createActionsPanel();
        
        centerPanel.add(devicesPanel);
        centerPanel.add(actionsPanel);
        
        // === PROGRESS BAR ===
        JPanel progressPanel = createProgressPanel();
        
        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        setVisible(true);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PRIMARY);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("Transferência LAN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        
        JLabel subtitle = new JLabel("Envie ficheiros rapidamente na rede local");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setBackground(BG_PRIMARY);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        
        header.add(titlePanel, BorderLayout.WEST);
        
        return header;
    }
    
    private JPanel createDevicesPanel() {
        JPanel panel = createCard();
        panel.setLayout(new BorderLayout(0, 15));
        
        JLabel title = new JLabel("Dispositivos Disponíveis");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        
        deviceModel = new DefaultListModel<>();
        deviceList = new JList<>(deviceModel);
        deviceList.setBackground(BG_SECONDARY);
        deviceList.setForeground(TEXT_PRIMARY);
        deviceList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Custom cell renderer
        deviceList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                
                label.setBorder(new EmptyBorder(10, 10, 10, 10));
                
                if (isSelected) {
                    label.setBackground(ACCENT);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(BG_SECONDARY);
                    label.setForeground(TEXT_PRIMARY);
                }
                
                Device dev = (Device) value;
                label.setText("🖥️  " + dev.name + " (" + dev.ip + ")");
                
                return label;
            }
        });
        
        JScrollPane scroll = new JScrollPane(deviceList);
        scroll.setBorder(BorderFactory.createLineBorder(BG_SECONDARY, 1));
        scroll.getViewport().setBackground(BG_SECONDARY);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BG_PRIMARY);
        
        // Card de ações
        JPanel actionsCard = createCard();
        actionsCard.setLayout(new GridLayout(4, 1, 0, 12));
        
        JLabel actionsTitle = new JLabel("Ações");
        actionsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        actionsTitle.setForeground(TEXT_PRIMARY);
        
        // Ficheiro selecionado
        selectedFileLabel = new JLabel("Nenhum ficheiro selecionado");
        selectedFileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        selectedFileLabel.setForeground(TEXT_SECONDARY);
        selectedFileLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        // Botões
        JButton chooseBtn = createModernButton("📁 Escolher Ficheiro", ACCENT, false);
        chooseBtn.addActionListener(e -> chooseFile());
        
        sendBtn = createModernButton("📤 Enviar", SUCCESS, false);
        sendBtn.addActionListener(e -> sendFile());
        
        JButton cancelBtn = createModernButton("✖ Cancelar", ERROR, false);
        cancelBtn.addActionListener(e -> {
            if (fileReceiver != null) {
                fileReceiver.cancelTransfer();
            }
        });
        
        actionsCard.add(actionsTitle);
        actionsCard.add(selectedFileLabel);
        actionsCard.add(chooseBtn);
        
        JPanel sendCancelPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        sendCancelPanel.setBackground(BG_CARD);
        sendCancelPanel.add(sendBtn);
        sendCancelPanel.add(cancelBtn);
        actionsCard.add(sendCancelPanel);
        
        // Card de log
        JPanel logCard = createCard();
        logCard.setLayout(new BorderLayout(0, 10));
        
        JLabel logTitle = new JLabel("Registo de Atividade");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logTitle.setForeground(TEXT_PRIMARY);
        
        log = new JTextArea();
        log.setBackground(BG_SECONDARY);
        log.setForeground(TEXT_PRIMARY);
        log.setFont(new Font("Consolas", Font.PLAIN, 12));
        log.setEditable(false);
        log.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createLineBorder(BG_SECONDARY, 1));
        logScroll.getViewport().setBackground(BG_SECONDARY);
        
        logCard.add(logTitle, BorderLayout.NORTH);
        logCard.add(logScroll, BorderLayout.CENTER);
        
        panel.add(actionsCard, BorderLayout.NORTH);
        panel.add(logCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createProgressPanel() {
        JPanel panel = createCard();
        panel.setLayout(new BorderLayout(0, 8));
        
        JLabel label = new JLabel("Progresso");
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_PRIMARY);
        
        progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progress.setForeground(ACCENT);
        progress.setBackground(BG_SECONDARY);
        progress.setBorder(new EmptyBorder(5, 5, 5, 5));
        progress.setPreferredSize(new Dimension(0, 35));
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(progress, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(60, 60, 60), 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        return card;
    }
    
    private JButton createModernButton(String text, Color color, boolean outline) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(!outline);
        btn.setContentAreaFilled(!outline);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 45));
        
        if (outline) {
            btn.setForeground(color);
            btn.setBorder(new LineBorder(color, 2, true));
        } else {
            btn.setBackground(color);
            btn.setForeground(Color.WHITE);
            btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        }
        
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!outline) {
                    btn.setBackground(color.brighter());
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!outline) {
                    btn.setBackground(color);
                }
            }
        });
        
        return btn;
    }
    
    public void setFileReceiver(FileReceiver receiver) {
        this.fileReceiver = receiver;
    }
    
    public void updateProgress(long received, long total) {
        int percent = (int) ((received * 100) / total);
        SwingUtilities.invokeLater(() -> {
            progress.setValue(percent);
            progress.setString(percent + "%");
        });
    }

    public void addDevice(String deviceInfo) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = deviceInfo.split("\\|");
            String name = parts[1];
            int port = Integer.parseInt(parts[2]);
            String ip = parts[3];

            for (int i = 0; i < deviceModel.size(); i++) {
                if (deviceModel.get(i).ip.equals(ip)) return;
            }

            deviceModel.addElement(new Device(name, ip, port));
            log.append("✓ Dispositivo encontrado: " + name + " (" + ip + ")\n");
        });
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();
            selectedFileLabel.setText("📄 " + selected.getName());
            selectedFileLabel.setForeground(SUCCESS);
            log.append("✓ Selecionado: " + selected.getName() + "\n");
        }
    }

    private void sendFile() {
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    "Nenhum ficheiro/pasta selecionado",
                    "Erro",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Device device = deviceList.getSelectedValue();
        if (device == null) {
            JOptionPane.showMessageDialog(this,
                    "Nenhum dispositivo selecionado",
                    "Erro",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        progress.setValue(0);
        log.append("⏳ A enviar para " + device.ip + "...\n");

        new Thread(() -> {
            try {
                FileSender.send(selected, device.ip, progress);
                SwingUtilities.invokeLater(() ->
                    log.append("✓ Enviado para " + device.ip + "\n")
                );
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                    log.append("✖ Erro ao enviar: " + e.getMessage() + "\n")
                );
            }
        }).start();
    }
}