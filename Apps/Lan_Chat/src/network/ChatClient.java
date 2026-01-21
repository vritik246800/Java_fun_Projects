package network;

import java.io.*;
import java.net.*;

public class ChatClient {

    public static void send(String ip, String message) {
        try {
            Socket socket = new Socket(ip, 9999);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
