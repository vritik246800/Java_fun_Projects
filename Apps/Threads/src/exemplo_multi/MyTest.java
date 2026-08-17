package exemplo_multi;

public class MyTest {
	static int i = 2;
	void main() {
		Tarefa t1=new Tarefa(0,10);
		Tarefa t2=new Tarefa(10,20);
		
		new Thread(t1).start();
		new Thread(t2).start();
		
		countMe("Vritik: ");
	}
	private static void countMe(String name) {
		i++;
		IO.println("Contador Corrente e "+i+", "+"actualizado por: " +name);
	}
}