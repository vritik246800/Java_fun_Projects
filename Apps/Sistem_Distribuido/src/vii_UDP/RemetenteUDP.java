package vii_UDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RemetenteUDP {
	

	void main(String[] args) {
		if(args.length != 3) {
			IO.println("Uso correto: <Nome da maquina> <Porta> <Mensagem>");
			System.exit(0);
		}
		try {
			//Primeiro argumento é o nome do host destino
			InetAddress addr = InetAddress.getByName(args[0]);
			
			int port = Integer.parseInt(args[1]);
			
			byte[] msg = args[2].getBytes();
			
			//Monta o pacote a ser enviado
			DatagramPacket pkg = new DatagramPacket(msg,msg.length, addr, port);
			
			// Cria o DatagramSocket que será responsável por enviar a mensagem
			DatagramSocket ds = new DatagramSocket();
			
			//Envia a mensagem
			ds.send(pkg);
			IO.println("Mensagem enviada para: " + addr.getHostAddress() +"\n" +"Porta: " + port + "\n" + "Mensagem: " + args[2]);
			
			//Fecha o DatagramSocket
			ds.close();
		}
		catch(IOException ioe) {
			IO.println(ioe.getMessage());
		}
	
	}
}