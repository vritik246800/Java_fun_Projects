package viiii_RMI;

import java.rmi.Naming;

public class HelloClient {
    public static void main(String[] args) {
        try {
            String host = (args.length > 0) ? args[0] : "10.206.140.201";
            // IP : Ventura
            HelloWorld obj = (HelloWorld) Naming.lookup("//" + host + "/HelloWorld");
            System.out.println("Mensagem do Servidor: " + obj.hello());
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }
}
