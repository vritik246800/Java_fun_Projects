package discovery;

import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class UdpDiscovery extends Thread {

    private static final int PORT = 8888;
    private static final int HEARTBEAT_INTERVAL = 3000; // 3s

    private String username;
    private Consumer<Map<String, Long>> onUsersUpdate;

    private Map<String, Long> onlineUsers = new ConcurrentHashMap<>();

    public UdpDiscovery(String username, Consumer<Map<String, Long>> onUsersUpdate) {
        this.username = username;
        this.onUsersUpdate = onUsersUpdate;
    }

    @Override
    public void run() {
        new Thread(this::sendHeartbeat).start();

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] buffer = new byte[256];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.startsWith("ONLINE|")) {
                    String user = msg.substring(7);
                    String key = user + "@" + packet.getAddress().getHostAddress();

                    onlineUsers.put(key, System.currentTimeMillis());
                    onUsersUpdate.accept(onlineUsers);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendHeartbeat() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            while (true) {
                byte[] data = ("ONLINE|" + username).getBytes();
                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        InetAddress.getByName("255.255.255.255"),
                        PORT
                );
                socket.send(packet);
                Thread.sleep(HEARTBEAT_INTERVAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
