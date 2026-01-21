package network;

import javax.sound.sampled.*;
import javax.swing.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;

public class AudioReceiver extends Thread {

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            ServerSocket server = new ServerSocket(10000);

            while (true) { // 🔥 MUITO IMPORTANTE
                Socket client = server.accept();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    utils.TrayNotification.show(
                        "Áudio",
                        "🎙️ Mensagem de áudio recebida"
                    );
                });
                /*
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        null,
                        "🎙️ A receber mensagem de áudio...",
                        "Áudio",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    );
                });*/
                
                InputStream in = client.getInputStream();

                byte[] buffer = new byte[4096];
                int bytes;

                while ((bytes = in.read(buffer)) > 0) {
                    speakers.write(buffer, 0, bytes);
                }

                client.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
