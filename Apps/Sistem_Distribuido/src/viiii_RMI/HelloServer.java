package viiii_RMI;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

public class HelloServer extends UnicastRemoteObject implements HelloWorld {
	public HelloServer() throws RemoteException {
		super();
	}

	// static: main de instância faria o launcher criar (e exportar) um HelloServer extra
	public static void main(String [] args) {
		try {
			// arranca o rmiregistry dentro desta JVM; se já houver um a correr (porta 1099 ocupada), usa esse
			try {
				LocateRegistry.createRegistry(1099);
			} catch (ExportException e) {
				IO.println("rmiregistry já a correr na porta 1099");
			}
			HelloServer obj = new HelloServer();
			Naming.rebind("//localhost/HelloWorld", obj);
			IO.println("HelloServer registado como //localhost/HelloWorld");
		} catch (Exception ex) {
			IO.println("Exception: " + ex.getMessage());
		}
	}

	public String hello() throws RemoteException {
		IO.println("Executando hello()");
		return "Hello!!!";
	}
}
