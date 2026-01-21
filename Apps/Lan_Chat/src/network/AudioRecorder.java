package network;

import javax.sound.sampled.*;
import java.io.File;

public class AudioRecorder {

    private TargetDataLine mic;
    private File file;

    public AudioRecorder(File file) {
        this.file = file;
    }

    public void start() throws Exception {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        mic = AudioSystem.getTargetDataLine(format);
        mic.open(format);
        mic.start();

        new Thread(() -> {
            try {
                AudioInputStream ais = new AudioInputStream(mic);
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        mic.stop();
        mic.close();
    }
}
