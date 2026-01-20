package network;

import utils.ZipUtils;
import utils.HashUtils;

import java.io.*;
import java.net.*;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class FileSender {

    public static void send(File file, String ip, JProgressBar bar) throws Exception {

        boolean isDir = file.isDirectory();
        File toSend = file;

        if (isDir) {
            toSend = ZipUtils.zipDirectory(file);
        }

        Socket socket = new Socket(ip, 9999);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        // 🔹 HASH
        String hash = HashUtils.sha256(toSend);

        // 🔹 HEADER (MESMA ORDEM DO RECEIVER)
        out.writeUTF(isDir ? "DIR" : "FILE");
        out.writeUTF(toSend.getName());
        out.writeLong(toSend.length());
        out.writeUTF(hash);

        out.flush();

        // 🔹 Esperar resposta
        String response = in.readUTF();
        if (!response.equals("ACCEPT")) {
            socket.close();
            return;
        }

        // 🔹 Enviar dados + progresso
        FileInputStream fis = new FileInputStream(toSend);
        byte[] buffer = new byte[8192];
        long sent = 0;
        long total = toSend.length();
        int read;

        while ((read = fis.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            sent += read;

            int percent = (int) ((sent * 100) / total);
            SwingUtilities.invokeLater(() -> bar.setValue(percent));
        }

        fis.close();
        socket.close();

        if (isDir) {
            toSend.delete();
        }
    }
}
