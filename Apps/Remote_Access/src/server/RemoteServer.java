package server;

import crypto.CryptoUtils;

import javax.crypto.CipherOutputStream;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class RemoteServer implements Runnable {

    private volatile boolean running = true;
    private ServerSocket server;
    private final int port;
    private final PrintWriter log;

    public RemoteServer(int port, PrintWriter log) {
        this.port = port;
        this.log = log;
    }

    public void stopServer() {
        running = false;
        try {
            if (server != null) server.close();
        } catch (IOException ignored) {}
        log.println("🛑 Servidor parado");
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(port);
            log.println("✅ Servidor LAN ativo na porta " + port);

            Socket socket = server.accept();
            log.println("🔗 Cliente conectado: " + socket.getInetAddress());

            Robot robot = new Robot();
            Rectangle screen =
                    new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            CipherOutputStream cipherOut =
                    new CipherOutputStream(
                            socket.getOutputStream(),
                            CryptoUtils.cipher(javax.crypto.Cipher.ENCRYPT_MODE)
                    );

            DataOutputStream out =
                    new DataOutputStream(new BufferedOutputStream(cipherOut));

            while (running) {
                BufferedImage img = robot.createScreenCapture(screen);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);

                byte[] data = baos.toByteArray();
                out.writeInt(data.length);
                out.write(data);
                out.flush();

                Thread.sleep(33);
            }

        } catch (Exception e) {
            log.println("❌ Servidor erro: " + e.getMessage());
        }
    }
}
