package utils;

import java.io.*;
import java.util.zip.*;

public class ConversationExport {

    public static void export(String userAtIp, File outZip) {
        try {
            String safe = FileUtils.sanitize(userAtIp.replace("@", "_"));
            File f = new File("history", safe + ".txt");

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZip))) {
                zos.putNextEntry(new ZipEntry(f.getName()));
                try (FileInputStream fis = new FileInputStream(f)) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(String userAtIp) {
        String safe = FileUtils.sanitize(userAtIp.replace("@", "_"));
        new File("history", safe + ".txt").delete();
    }
}
