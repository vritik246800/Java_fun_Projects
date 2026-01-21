package network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class ChatServer extends Thread {

    private Consumer<String> onMessage;

    public ChatServer(Consumer<String> onMessage) {
        this.onMessage = onMessage;
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(9999);

            while (true) {
                Socket client = server.accept();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream())
                );

                String msg = in.readLine();
                if (msg != null) {
                	String ip = client.getInetAddress().getHostAddress();
                	onMessage.accept(ip + "|" + msg);

                    javax.swing.SwingUtilities.invokeLater(() -> {
                        utils.TrayNotification.show(
                            "Mensagem",
                            "📩 Nova mensagem recebida"
                        );
                    });
                    /*
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "📩 Nova mensagem recebida!",
                            "Mensagem",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE
                        );
                    });*/
                }
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
