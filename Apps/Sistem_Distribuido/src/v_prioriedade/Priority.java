package v_prioriedade;

class Priority implements Runnable {
	
	int count;
	
	Thread thrd;
	
	static boolean stop = false;
	
	static String currentName;
	// Construtor para criar uma nova thread
	
	Priority(String name) {
		thrd = new Thread(this, name);
		count = 0;
		currentName = name;
	}
	// Define o comportamento da thread
	public void run() {
		
		IO.println(thrd.getName() + " starting.");
		
		do {
			count++;
		
			if (!currentName.equals(thrd.getName())) {
				
				currentName = thrd.getName();
				
				IO.println("| " + count + " In " + currentName + " : _-_-" + "\n" + "|" + "-".repeat(25) + "|");
			}
		
		} while (!stop && count < 10000000);
		
		stop = true; // Interrompe outras threads ao alcançar 10.000.000
		
		IO.println("\n" + thrd.getName() + " terminating.");
	}
}
