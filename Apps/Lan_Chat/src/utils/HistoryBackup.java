package utils;

import java.io.*;
import java.util.zip.*;

public class HistoryBackup {

    public static void backupNow() {
        File src = new File("history");
        File dstDir = new File("backup");
        if (!dstDir.exists()) dstDir.mkdirs();

        File zip = new File(dstDir, "history_" + System.currentTimeMillis() + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            for (File f : src.listFiles()) {
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
}
