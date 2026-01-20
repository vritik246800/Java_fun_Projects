package object;
public class Device {
    public String name;
    public String ip;
    public int port;

    public Device(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return name + " (" + ip + ")";
    }
}
