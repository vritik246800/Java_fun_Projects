package compartilhada;

class TestThread {
	
	void printName() {
		
		IO.println("Thread Name: "+ Thread.currentThread().getName());
		IO.println("Thread Prioriedade: "+ Thread.currentThread().getPriority());
	}
	
	void main() {
		TestThread t=new TestThread();
		t.printName();
	}

}
