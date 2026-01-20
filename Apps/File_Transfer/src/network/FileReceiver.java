package network;

import utils.ZipUtils;
import utils.HashUtils;
import ui.MainWindow;
import ui.PedidoTransferenciaDialog;

import javax.swing.*;
import java.io.*;
import java.net.*;

public class FileReceiver implements Runnable {

    private static final int PORT = 9999;

    private volatile boolean cancelRequested = false;
    private final MainWindow mainWindow;

    public FileReceiver(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void cancelTransfer() {
        cancelRequested = true;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = server.accept();
                receive(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receive(Socket socket) {
        cancelRequested = false;

        File output = null;

        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            // ───────────────── HEADER ─────────────────
            String type = in.readUTF(); // FILE ou DIR
            String name = in.readUTF();
            long size = in.readLong();
            String originalHash = in.readUTF();

            final boolean[] accepted = { false };
            final File[] destino = { null };

            // ───────────── CONFIRMAÇÃO + ESCOLHA DO PATH ─────────────
            SwingUtilities.invokeAndWait(() -> {
                new PedidoTransferenciaDialog(
                    mainWindow,
                    name,
                    () -> {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setDialogTitle("Guardar ficheiro como");
                        chooser.setSelectedFile(new File(name));

                        int result = chooser.showSaveDialog(mainWindow);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            destino[0] = chooser.getSelectedFile();
                            accepted[0] = true;
                            try {
                                out.writeUTF("ACCEPT");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                out.writeUTF("REJECT");
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    () -> {
                        try {
                            out.writeUTF("REJECT");
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                ).setVisible(true);
            });

            if (!accepted[0] || destino[0] == null) {
                return;
            }

            output = destino[0];

            // ───────────────── RECEBER DADOS ─────────────────
            try (FileOutputStream fos = new FileOutputStream(output)) {

                byte[] buffer = new byte[8192];
                long received = 0;

                while (received < size && !cancelRequested) {
                    int read = in.read(buffer);
                    if (read == -1) break;

                    fos.write(buffer, 0, read);
                    received += read;

                    mainWindow.updateProgress(received, size);
                }
            }

            if (cancelRequested) {
                if (output.exists()) output.delete();
                socket.close();
                return;
            }

            // ───────────────── VERIFICAR HASH ─────────────────
            String receivedHash = HashUtils.sha256(output);
            if (!receivedHash.equals(originalHash)) {
                JOptionPane.showMessageDialog(
                        mainWindow,
                        "ERRO: ficheiro corrompido!",
                        "Erro",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // ───────────────── DESCOMPACTAR ─────────────────
            if (type.equals("DIR")) {
                File destDir = new File(output.getParent(), name.replace(".zip", ""));
                ZipUtils.unzip(output, destDir);
                output.delete();
            }

            JOptionPane.showMessageDialog(
                    mainWindow,
                    "Transferência concluída!",
                    "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            e.printStackTrace();
            if (output != null && output.exists()) {
                output.delete();
            }
        }
    }
}
