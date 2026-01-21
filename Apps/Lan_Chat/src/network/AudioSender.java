package network;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;

public class AudioSender implements Runnable {

    private volatile boolean recording = true;
    private String ip;

    public AudioSender(String ip) {
        this.ip = ip;
    }

    public void stopRecording() {
        recording = false;
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

            while (recording) {
                int bytes = mic.read(buffer, 0, buffer.length);
                out.write(buffer, 0, bytes);
            }

            mic.stop();
            mic.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
