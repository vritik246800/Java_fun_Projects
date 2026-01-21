package network;

import javax.sound.sampled.*;
import java.net.*;
import java.io.InputStream;

public class CallReceiver extends Thread {

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            ServerSocket server = new ServerSocket(10001);

            while (true) {
            	Socket client = server.accept();
            	javax.swing.SwingUtilities.invokeLater(() -> {
            	    utils.TrayNotification.show(
            	        "Chamada",
            	        "📞 Chamada recebida"
            	    );
            	});
            	/*
            	int option = javax.swing.JOptionPane.showConfirmDialog(
            	    null,
            	    "📞 Chamada recebida. Aceitar?",
            	    "Chamada",
            	    javax.swing.JOptionPane.YES_NO_OPTION
            	);

            	if (option != javax.swing.JOptionPane.YES_OPTION) {
            	    client.close();
            	    continue;
            	}*/

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
