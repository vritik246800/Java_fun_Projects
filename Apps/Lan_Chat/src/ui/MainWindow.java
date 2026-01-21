package ui;

import discovery.UdpDiscovery;
import network.ChatClient;
import network.ChatServer;
import network.AudioSender;
import network.AudioReceiver;
import network.CallSender;
import utils.ChatHistory;
import utils.TrayNotification;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

public class MainWindow extends JFrame {

    private DefaultListModel<String> usersModel = new DefaultListModel<>();
    private JList<String> usersList = new JList<>(usersModel);
    private Map<String, Integer> unread = new HashMap<>();

    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private JLabel statusLabel = new JLabel("● Online");

    private JButton sendButton = new JButton("Enviar");
    private JButton talkButton = new JButton("🎤");
    private JButton callButton = new JButton("📞");

    private String username;
    private String currentChat = null;

    // Áudio mensagem
    private boolean isRecording = false;
    private AudioSender audioSender;
    private Thread audioThread;

    // Chamada
    private boolean inCall = false;
    private CallSender callSender;
    private Thread callThread;

    // Receptor
    private AudioReceiver audioReceiver;

    public MainWindow(String username) {
        this.username = username;

        setTitle("LAN Chat - " + username);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        buildUI();
        TrayNotification.init();
        startNetwork();
    }

    // ================= UI =================

    private void buildUI() {
        setLayout(new BorderLayout());
        
        // Painel lateral de utilizadores
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(45, 45, 48));
        leftPanel.setPreferredSize(new Dimension(220, 0));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 5));

        JLabel usersTitle = new JLabel("Utilizadores");
        usersTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        usersTitle.setForeground(Color.WHITE);
        usersTitle.setBorder(new EmptyBorder(0, 5, 10, 0));
        leftPanel.add(usersTitle, BorderLayout.NORTH);

        usersList.setBackground(new Color(37, 37, 38));
        usersList.setForeground(Color.WHITE);
        usersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usersList.setSelectionBackground(new Color(0, 120, 215));
        usersList.setSelectionForeground(Color.WHITE);
        usersList.setBorder(new EmptyBorder(5, 5, 5, 5));
        usersList.setFixedCellHeight(35);
        usersList.setCellRenderer(new UserCellRenderer());
        
        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(usersScroll, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        // Painel central de chat
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);

        // Header do chat
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(new Color(240, 240, 240));
        chatHeader.setBorder(new EmptyBorder(12, 15, 12, 15));
        chatHeader.setPreferredSize(new Dimension(0, 55));

        JLabel chatTitle = new JLabel("Seleciona um utilizador");
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chatTitle.setForeground(new Color(50, 50, 50));
        chatHeader.add(chatTitle, BorderLayout.WEST);

        JPanel callControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        callControls.setOpaque(false);
        
        talkButton.setPreferredSize(new Dimension(45, 35));
        callButton.setPreferredSize(new Dimension(45, 35));
        
        styleButton(talkButton, new Color(76, 175, 80));
        styleButton(callButton, new Color(33, 150, 243));
        
        talkButton.setEnabled(false);
        callButton.setEnabled(false);
        
        callControls.add(talkButton);
        callControls.add(callButton);
        chatHeader.add(callControls, BorderLayout.EAST);

        centerPanel.add(chatHeader, BorderLayout.NORTH);

        // Área de mensagens
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        chatArea.setBackground(Color.WHITE);
        
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        centerPanel.add(chatScroll, BorderLayout.CENTER);

        // Painel de entrada
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(new EmptyBorder(10, 15, 15, 15));
        inputPanel.setBackground(Color.WHITE);

        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(8, 12, 8, 12)
        ));

        sendButton.setPreferredSize(new Dimension(90, 38));
        styleButton(sendButton, new Color(0, 120, 215));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Barra de status
        statusLabel.setBorder(new EmptyBorder(8, 15, 8, 15));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(76, 175, 80));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(250, 250, 250));
        add(statusLabel, BorderLayout.SOUTH);

        // Event listeners
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        talkButton.addActionListener(e -> toggleAudioMessage());
        callButton.addActionListener(e -> toggleCall());
        usersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {

                String selected = usersList.getSelectedValue();
                if (selected == null) return;

                // 🔥 definir conversa atual
                currentChat = selected;

                // 🔥 LIMPAR NÃO LIDAS (ISTO FALTAVA)
                unread.remove(currentChat);

                // carregar histórico
                chatArea.setText(ChatHistory.load(currentChat));
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }
    
    private String withPreviewFromIndex(String key, String user, String ip, String lastMsg) {
        int count = unread.getOrDefault(user + "@" + ip, 0);
        String badge = count > 0 ? " (" + count + ")" : "";
        return user + badge + (lastMsg.isEmpty() ? "" : " | " + lastMsg);
    }
    

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
    }

    // ================= AUDIO MESSAGE =================

    private void toggleAudioMessage() {
        String selected = usersList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Seleciona um utilizador.");
            return;
        }

        String ip = selected.split("@")[1];

        if (!isRecording) {
            isRecording = true;
            statusLabel.setText("● A gravar áudio...");
            statusLabel.setForeground(Color.RED);
            talkButton.setText("⏹");
            talkButton.setBackground(new Color(244, 67, 54));
            
            audioSender = new AudioSender(ip);
            audioThread = new Thread(audioSender);
            audioThread.start();

        } else {
            isRecording = false;
            statusLabel.setText("● Online");
            statusLabel.setForeground(new Color(76, 175, 80));
            talkButton.setText("🎤");
            talkButton.setBackground(new Color(76, 175, 80));

            if (audioSender != null) {
                audioSender.stopRecording();
            }
        }
    }

    // ================= CALL =================

    private void toggleCall() {
        String selected = usersList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Seleciona um utilizador.");
            return;
        }

        String ip = selected.split("@")[1];

        if (!inCall) {
            inCall = true;
            statusLabel.setText("● Em chamada...");
            statusLabel.setForeground(new Color(255, 152, 0));
            callButton.setText("⏹");
            callButton.setBackground(new Color(244, 67, 54));

            callSender = new CallSender(ip);
            callThread = new Thread(callSender);
            callThread.start();

        } else {
            inCall = false;
            statusLabel.setText("● Online");
            statusLabel.setForeground(new Color(76, 175, 80));
            callButton.setText("📞");
            callButton.setBackground(new Color(33, 150, 243));

            if (callSender != null) {
                callSender.stopCall();
            }
        }
    }

    // ================= NETWORK =================

    private void startNetwork() {

        // 🔹 UM ÚNICO servidor de chat
        new ChatServer(data -> SwingUtilities.invokeLater(() -> {

            // data = "ip|mensagem"
            String[] parts = data.split("\\|", 2);
            String ip = parts[0];
            String text = parts[1];

            String sender = findUserByHistory(ip);

            ChatHistory.save(sender, "📩 " + text);
            
            if (!sender.equals(currentChat)) {
                unread.put(sender, unread.getOrDefault(sender, 0) + 1);
            }

            if (sender.equals(currentChat)) {
                chatArea.setText(ChatHistory.load(sender));
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            } else {
                unread.put(sender, unread.getOrDefault(sender, 0) + 1);
                TrayNotification.show(
                    "Mensagem",
                    "📩 Nova mensagem de " + sender.split("@")[0]
                );
            }

            statusLabel.setText("● Nova mensagem");
            statusLabel.setForeground(new Color(33, 150, 243));

        })).start();

        // 🔹 Receptor de áudio (UMA VEZ)
        audioReceiver = new AudioReceiver();
        audioReceiver.start();

        // 🔹 Descoberta de utilizadores (heartbeat UDP)
        new UdpDiscovery(username, users -> SwingUtilities.invokeLater(() -> {

            long now = System.currentTimeMillis();
            usersModel.clear();

            users.forEach((user, lastSeen) -> {
                if (now - lastSeen < 7000) {
                	usersModel.addElement(user);
                }
            });

        })).start();
        
        new javax.swing.Timer(5 * 60 * 1000, e -> {
            utils.HistoryBackup.backupNow();
        }).start();

    }

    // ================= CHAT =================

    private void sendMessage() {
        if (currentChat == null) return;

        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        String ip = currentChat.split("@")[1];
        ChatClient.send(ip, msg);

        ChatHistory.save(currentChat, "💬 Eu: " + msg);
        chatArea.setText(ChatHistory.load(currentChat));
        chatArea.setCaretPosition(chatArea.getDocument().getLength());

        inputField.setText("");
    }
    
    private String findUserByHistory(String ip) {
        File dir = new File("history");
        File[] files = dir.listFiles((d, name) -> name.contains("_" + ip.replace(".", "_")));

        if (files != null && files.length > 0) {
            return files[0].getName().replace(".txt", "")
                    .replace("_", "@"); // volta ao formato user@ip
        }
        return ip;
    }

    private void loadHistory() {
        if (currentChat != null) {
            chatArea.setText(ChatHistory.load(currentChat));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

}