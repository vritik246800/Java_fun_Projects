package jotes.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Armazenamento de ficheiros anexados às notas.
 * Os ficheiros vivem fora da SQLite, em {@code <pasta da bd>/attachments/<noteId>/<ficheiro>}.
 */
public class AttachmentStorage {
    private final Path root;

    public AttachmentStorage(Path dataDir) {
        this.root = dataDir.resolve("attachments");
    }

    public Path root() { return root; }

    /** Copia um ficheiro para a loja; devolve o caminho relativo à raiz. */
    public String copyIn(long noteId, Path source) throws IOException {
        Path dir = root.resolve(String.valueOf(noteId));
        Files.createDirectories(dir);

        String name = source.getFileName().toString();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }

        Path dest = dir.resolve(name);
        int i = 2;
        while (Files.exists(dest)) {
            dest = dir.resolve(base + " (" + i++ + ")" + ext);
        }
        Files.copy(source, dest);
        return root.relativize(dest).toString().replace('\\', '/');
    }

    /** Resolve um caminho relativo guardado na BD para um caminho absoluto. */
    public Path resolve(String relpath) {
        return root.resolve(relpath);
    }

    public boolean exists(String relpath) {
        return Files.exists(resolve(relpath));
    }

    public void delete(String relpath) throws IOException {
        Files.deleteIfExists(resolve(relpath));
    }

    public long size(String relpath) throws IOException {
        return Files.size(resolve(relpath));
    }
}
