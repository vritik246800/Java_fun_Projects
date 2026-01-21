package utils;

public class FileUtils {

    public static String sanitize(String input) {
        if (input == null) return "unknown";

        return input
                .toLowerCase()
                .replace("@", "_")
                .replace(".", "_")
                .replace(":", "_")
                .replaceAll("[^a-z0-9_]", "");
    }
}
