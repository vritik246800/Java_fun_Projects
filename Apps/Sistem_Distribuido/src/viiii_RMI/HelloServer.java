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

    @Override
    public String hello() throws RemoteException {
        System.out.println("Executando hello()");
        return "Hello Ventura!!!";
    }

    public static void main(String[] args) {
        try {
        		try {
        		LocateRegistry.createRegistry(1099);	
        		}catch(ExportException e) {
        			System.out.println("Rmi já está a correr");
        		}	
            HelloServer obj = new HelloServer();
            //IP : 10.206.140.91
            Naming.rebind("//10.206.140.91/HelloWorld", obj);
            System.out.println("Servidor RMI pronto...");
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }
}