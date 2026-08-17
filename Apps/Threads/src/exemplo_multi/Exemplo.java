package exemplo_multi;

class Exemplo {
	void main() {
		Tarefa t1=new Tarefa(0,10);
		t1.setName("Tarefa 1");
		Tarefa t2=new Tarefa(11,20);
		t2.setName("Tarefa 2");
		Tarefa t3=new Tarefa(21,30);
		t3.setName("Tarefa 3");
		
		t1.start();
		t2.start();
		t3.start();
		
		try {
			t1.join();
			t2.join();
			t3.join();
		}catch (InterruptedException ex) {
			ex.getLocalizedMessage();
		}
		
		IO.println("Total: "+ (t1.getTotal() + t2.getTotal() + t3.getTotal()));
	}

}
