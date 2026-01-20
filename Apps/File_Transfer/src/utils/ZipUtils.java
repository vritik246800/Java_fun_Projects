package utils;

import java.io.*;
import java.util.zip.*;

public class ZipUtils {

    public static File zipDirectory(File dir) throws IOException {
        File zipFile = new File(dir.getName() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        zip(dir, dir.getName(), zos);
        zos.close();
        return zipFile;
    }

    private static void zip(File file, String name, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                zip(f, name + "/" + f.getName(), zos);
            }
        } else {
            zos.putNextEntry(new ZipEntry(name));
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, read);
            }
            fis.close();
            zos.closeEntry();
        }
    }

    public static void unzip(File zipFile, File dest) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            File file = new File(dest, entry.getName());
            file.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
        }
        zis.close();
    }
}
