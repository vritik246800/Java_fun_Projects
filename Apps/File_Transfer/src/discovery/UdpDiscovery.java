package discovery;

import java.net.*;
import java.util.function.Consumer;

public class UdpDiscovery implements Runnable {

    private static final int PORT = 8888;
    private Consumer<String> onDeviceFound;

    public UdpDiscovery(Consumer<String> callback) {
        this.onDeviceFound = callback;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket();) {
            socket.setBroadcast(true);

            // Enviar pedido
            String msg = "DISCOVER_REQUEST|" + InetAddress.getLocalHost().getHostName() + "|9999";
            byte[] data = msg.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data, data.length,
                    InetAddress.getByName("255.255.255.255"), PORT
            );
            socket.send(packet);

            // Escutar respostas
            while (true) {
                byte[] buf = new byte[256];
                DatagramPacket response = new DatagramPacket(buf, buf.length);
                socket.receive(response);

                String text = new String(response.getData()).trim();
                if (text.startsWith("DISCOVER_RESPONSE")) {
                    onDeviceFound.accept(text + "|" + response.getAddress().getHostAddress());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
