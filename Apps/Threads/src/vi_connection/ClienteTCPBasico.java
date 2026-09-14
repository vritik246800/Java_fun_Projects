package vi_connection;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Date;
import javax.swing.JOptionPane;

public class ClienteTCPBasico {
	public static void main(String[] args) {
		try {
			// A ficha usa 12245 (gralha) — o servidor ouve na 12345
			Socket cliente = new Socket("localhost", 12345);
			ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());
			Date data_atual = (Date) entrada.readObject();
			System.out.println("Data recebida do servidor: " + data_atual);
			JOptionPane.showMessageDialog(null, "Data recebida do servidor:" + data_atual.toString());
			entrada.close();
			System.out.println("Conexão encerrada");
		} catch (Exception e) {
			System.out.println("Erro: " + e.getMessage());
		}
	}
}
