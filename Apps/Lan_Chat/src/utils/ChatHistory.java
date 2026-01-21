package utils;

import java.io.*;
import java.nio.file.Files;

public class ChatHistory {

    private static final File DIR = new File("history");

    static {
        if (!DIR.exists()) DIR.mkdirs();
    }

    private static File fileFor(String user, String ip) {
        String name = FileUtils.sanitize(user + "_" + ip) + ".txt";
        return new File(DIR, name);
    }

    public static void save(String userAtIp, String message) {
        try {
            String[] parts = userAtIp.split("@");
            String user = parts[0];
            String ip = parts.length > 1 ? parts[1] : "unknown";

            ConversationIndex.update(user, ip, message);

            File f = fileFor(user, ip);
            try (FileWriter fw = new FileWriter(f, true)) {
                fw.write(message + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String load(String userAtIp) {
        try {
            String[] parts = userAtIp.split("@");
            String user = parts[0];
            String ip = parts.length > 1 ? parts[1] : "unknown";

            File f = fileFor(user, ip);
            if (!f.exists()) return "";

            return Files.readString(f.toPath());
        } catch (Exception e) {
            return "";
        }
    }
}
