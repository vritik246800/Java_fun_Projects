package multithread;

class Task1 extends Thread{
	public void run() {
		IO.println("Task1 EXCEX");
	}
}

class Task2 extends Thread{
	public void run() {
		IO.println("Task2 EXCEX");
	}
}

class Main {
	void main() {
		Task1 t1=new Task1();
		Task2 t2=new Task2();
		
		t1.start();
		t2.start();
	}
}
