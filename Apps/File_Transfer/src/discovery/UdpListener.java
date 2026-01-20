package discovery;

import java.net.*;

public class UdpListener implements Runnable {

    private static final int PORT = 8888;
    private static final int TCP_PORT = 9999;

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {

            byte[] buffer = new byte[512];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());

                if (msg.startsWith("DISCOVER_REQUEST")) {

                    String pcName = InetAddress.getLocalHost().getHostName();
                    String response = "DISCOVER_RESPONSE|" + pcName + "|" + TCP_PORT;

                    byte[] data = response.getBytes();

                    DatagramPacket respPacket = new DatagramPacket(
                            data,
                            data.length,
                            packet.getAddress(),
                            packet.getPort()
                    );

                    socket.send(respPacket);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
