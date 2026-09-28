package viiii_RMI;

import java.rmi.Naming;

class HelloClient {
	void main(String[] args) {
		if (args.length < 1) {
			IO.println("Uso: java viiii_RMI.HelloClient <host>");
			System.exit(0);
		}
		try {
			HelloWorld obj = (HelloWorld) Naming.lookup("//" + args[0] + "/HelloWorld");
			IO.println("Mensagem do Servidor: " + obj.hello());
		} catch (Exception ex) {
			IO.println("Exception: " + ex.getMessage());
		}
	}
}
