package ui;

import client.RemoteClient;
import server.RemoteServer;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;

public class MainUI extends JFrame {

    private JTextArea logArea = new JTextArea();
    private RemoteServer server;
    private Thread serverThread;
    private RemoteClient client;
    private JTextField tfIP;
    private JTextField tfPort;
    private boolean fullscreen = false;

    public MainUI() {

        setTitle("Remote Desktop LAN");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        JProgressBar connectBar = new JProgressBar();
        connectBar.setIndeterminate(true);
        connectBar.setVisible(false);
        
        getContentPane().setBackground(new Color(30, 30, 30));

        logArea.setBackground(new Color(20, 20, 20));
        logArea.setForeground(new Color(0, 230, 118));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setCaretColor(Color.WHITE);

        // 1️⃣ CRIAR CAMPOS PRIMEIRO (ANTES DE QUALQUER add)
        tfIP   = new JTextField(getLocalIPAddress());
        tfPort = new JTextField("5000");

        // 2️⃣ PAINEL SUPERIOR (IP / PORTA)
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("IP / Host:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        topPanel.add(tfIP, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        topPanel.add(new JLabel("Porta:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        topPanel.add(tfPort, gbc);

        add(topPanel, BorderLayout.NORTH);

        // 3️⃣ ÁREA DE LOG
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        PrintWriter log = new PrintWriter(new WriterAdapter(logArea), true);

        // 4️⃣ BOTÕES
        JPanel buttons = new JPanel(new GridLayout(2, 3, 10, 10));

        AnimatedButton btnStartServer =
                new AnimatedButton("▶ START SERVER", new Color(46, 204, 113));

        AnimatedButton btnStopServer  =
                new AnimatedButton("⏹ STOP SERVER", new Color(231, 76, 60));

        AnimatedButton btnStartClient =
                new AnimatedButton("🔗 START CLIENT", new Color(46, 204, 113));

        AnimatedButton btnDisconnect  =
                new AnimatedButton("❌ DISCONNECT", new Color(155, 89, 182));

        AnimatedButton btnAutoIP      =
                new AnimatedButton("📡 IP AUTO", new Color(241, 196, 15));
        
        tfIP.setBackground(new Color(45,45,45));
        tfIP.setForeground(Color.WHITE);

        tfPort.setBackground(new Color(45,45,45));
        tfPort.setForeground(Color.WHITE);
        
        StatusLED serverLED = new StatusLED();
        LoadingSpinner spinner = new LoadingSpinner();
        spinner.setVisible(false);

        JToggleButton themeToggle = new JToggleButton("🌙");
        AnimatedButton fullscreenBtn =
                new AnimatedButton("🖥 FULLSCREEN", new Color(52,152,219));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.add(new JLabel("Server"));
        statusPanel.add(serverLED);
        statusPanel.add(spinner);
        statusPanel.add(themeToggle);
        statusPanel.add(fullscreenBtn);

        add(statusPanel, BorderLayout.WEST);

        buttons.add(btnStartServer);
        buttons.add(btnStopServer);
        buttons.add(btnStartClient);
        buttons.add(btnDisconnect);
        buttons.add(btnAutoIP);

        add(buttons, BorderLayout.SOUTH);
        
        btnStartServer.addActionListener(e -> {
            try {
                int port = Integer.parseInt(tfPort.getText());
                server = new RemoteServer(port, log);
                serverThread = new Thread(server);
                serverThread.start();

                serverLED.setOn(true);
                spinner.setVisible(true);

                log.println("🟢 Servidor ativo");

            } catch (Exception ex) {
                log.println("❌ Erro servidor");
            }
        });

        btnStopServer.addActionListener(e -> {
            if (server != null) {
                server.stopServer();
                serverLED.setOn(false);
                spinner.setVisible(false);
                log.println("🔴 Servidor parado");
            }
        });

        // 5️⃣ AÇÕES
        btnAutoIP.addActionListener(e -> {
            tfIP.setText(getLocalIPAddress());
            log.println("📡 IP LAN automático aplicado");
        });

        btnStartServer.addActionListener(e -> {
            try {
                int port = Integer.parseInt(tfPort.getText());
                server = new RemoteServer(port, log);
                serverThread = new Thread(server);
                serverThread.start();
            } catch (NumberFormatException ex) {
                log.println("❌ Porta inválida");
            }
        });

        btnStopServer.addActionListener(e -> {
            if (server != null) server.stopServer();
        });

        btnStartClient.addActionListener(e -> {
            try {
                if (client != null) {
                    log.println("⚠️ Cliente já conectado");
                    return;
                }

                String host = tfIP.getText().trim();
                int port = Integer.parseInt(tfPort.getText());

                client = new RemoteClient(host, port, log);
                btnStartClient.setEnabled(false);

            } catch (NumberFormatException ex) {
                log.println("❌ Porta inválida");
            } catch (Exception ex) {
                log.println("❌ Erro cliente: " + ex.getMessage());
            }
        });

        btnDisconnect.addActionListener(e -> {
            if (client != null) {
                client.disconnect();
                client = null;
                btnStartClient.setEnabled(true);
                log.println("🔌 Cliente desconectado");
            }
        });
        
        Theme[] current = { Theme.DARK };

        themeToggle.addActionListener(e -> {
            current[0] = current[0] == Theme.DARK ? Theme.LIGHT : Theme.DARK;
            getContentPane().setBackground(current[0].bg);
            logArea.setBackground(current[0].bg);
            logArea.setForeground(current[0].fg);
        });
        fullscreenBtn.addActionListener(e -> toggleFullscreen());

        setVisible(true);
    }
    private void toggleFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        dispose();
        setUndecorated(!fullscreen);

        if (!fullscreen) {
            gd.setFullScreenWindow(this);
        } else {
            gd.setFullScreenWindow(null);
            setSize(500, 400);
            setLocationRelativeTo(null);
        }

        setVisible(true);
        fullscreen = !fullscreen;
    }
    
    private String getLocalIPAddress() {
        try {
            var interfaces = java.net.NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                var ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;

                var addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var addr = addresses.nextElement();

                    if (addr instanceof java.net.Inet4Address
                            && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}

        return "localhost";
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(MainUI::new);
    }

}
