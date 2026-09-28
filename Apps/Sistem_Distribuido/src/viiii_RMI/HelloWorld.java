package viiii_RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HelloWorld extends Remote {
	String hello() throws RemoteException;
}
