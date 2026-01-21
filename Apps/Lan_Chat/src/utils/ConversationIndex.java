package utils;

import java.io.*;
import java.util.*;

public class ConversationIndex {

    private static final File DIR = new File("history");
    private static final File INDEX = new File(DIR, "index.txt");

    static {
        if (!DIR.exists()) DIR.mkdirs();
        try {
            if (!INDEX.exists()) INDEX.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void update(String user, String ip, String lastMsg) {
        String key = FileUtils.sanitize(user + "_" + ip);
        long now = System.currentTimeMillis();

        List<String> out = new ArrayList<>();
        boolean found = false;

        for (String line : readAll()) {
            String[] p = line.split("\\|", 5);
            if (p.length >= 3 && p[0].equals(key)) {
                out.add(key + "|" + user + "|" + ip + "|" + trim(lastMsg) + "|" + now);
                found = true;
            } else {
                out.add(line);
            }
        }

        if (!found) {
            out.add(key + "|" + user + "|" + ip + "|" + trim(lastMsg) + "|" + now);
        }

        writeAll(out);
    }

    public static synchronized List<String[]> readAllParsed() {
        List<String[]> list = new ArrayList<>();
        for (String l : readAll()) {
            String[] p = l.split("\\|", 5);
            if (p.length == 5) list.add(p);
        }
        return list;
    }

    private static List<String> readAll() {
        try {
            return java.nio.file.Files.readAllLines(INDEX.toPath());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static void writeAll(List<String> lines) {
        try {
            java.nio.file.Files.write(INDEX.toPath(), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() > 30 ? s.substring(0, 30) + "..." : s;
    }
}
