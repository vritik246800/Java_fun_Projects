package client;

import crypto.CryptoUtils;

import javax.crypto.CipherInputStream;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class RemoteClient extends JFrame {

    private Socket socket;
    private volatile boolean running = true;

    public RemoteClient(String host, int port, PrintWriter log) throws Exception{

        setTitle("Remote Client");
        setSize(900, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel screen = new JLabel();
        add(new JScrollPane(screen));
        setVisible(true);

        socket = new Socket(host, 5000);
        log.println("✅ Conectado ao servidor " + host);

        CipherInputStream cipherIn =
                new CipherInputStream(
                        socket.getInputStream(),
                        CryptoUtils.cipher(javax.crypto.Cipher.DECRYPT_MODE)
                );

        DataInputStream in =
                new DataInputStream(new BufferedInputStream(cipherIn));

        new Thread(() -> {
            try {
                while (running) {
                    int size = in.readInt();
                    byte[] data = in.readNBytes(size);

                    Image img = ImageIO.read(new ByteArrayInputStream(data));
                    Image scaled = img.getScaledInstance(
                            screen.getWidth(),
                            screen.getHeight(),
                            Image.SCALE_FAST
                    );

                    SwingUtilities.invokeLater(() ->
                            screen.setIcon(new ImageIcon(scaled))
                    );
                }
            } catch (Exception e) {
                log.println("❌ Cliente desconectado");
            }
        }).start();
    }

    public void disconnect() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
        dispose();
    }
}
