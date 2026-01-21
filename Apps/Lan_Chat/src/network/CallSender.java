package network;

import javax.sound.sampled.*;
import java.net.Socket;
import java.io.OutputStream;

public class CallSender implements Runnable {

    private volatile boolean active = true;
    private String ip;

    public CallSender(String ip) {
        this.ip = ip;
    }

    public void stopCall() {
        active = false;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            TargetDataLine mic = AudioSystem.getTargetDataLine(format);
            mic.open(format);
            mic.start();

            Socket socket = new Socket(ip, 10000);
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[4096];
            while (active) {
                int bytes = mic.read(buffer, 0, buffer.length);
                out.write(buffer, 0, bytes);
            }

            mic.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
