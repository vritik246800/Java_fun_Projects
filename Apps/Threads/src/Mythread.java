
public class Mythread extends Thread {
	
	public void run()
	{
	 	System.out.print("O thread esta em execução");
	}
	
	public static void main(String[]args)
	{
		Mythread mt=new Mythread();
		mt.start();
	}
}