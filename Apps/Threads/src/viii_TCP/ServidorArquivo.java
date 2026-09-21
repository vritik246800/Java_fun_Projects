package viii_TCP;

import java.net.ServerSocket;
import java.net.Socket;

public class ServidorArquivo {
	void main(String[] args) {
		if(args.length < 1) {
			IO.println("Informe a porta a ser ouvida pelo servidor");
			System.exit(0);
		}
		try {
			//Converte o parametro recebido para int (numero da porta)
			int port = Integer.parseInt(args[0]);
			IO.println("Inicializando o servidor...");

			//Inicializa o servidor
			ServerSocket serv = new ServerSocket(port);
			IO.println("Servidor iniciado, ouvindo a porta " + port);

			//Aguarda conexoes; cada cliente aceite e entregue a uma thread nova
			while(true) {
				Socket clie = serv.accept();
				//Inicia thread do cliente
				new ThreadCliente(clie).start();
			}
		}
		catch(Exception e) {
			IO.println("Erro no servidor: " + e.getMessage());
		}
	}
}
