package v_prioriedade;

class PriorityDemo
{
	void main() { 
		
		// Cria duas //threads com diferentes prioridades
		Priority mt1 = new Priority("High Priority"); 
		Priority mt2 = new Priority("Low Priority");
		
		// Configura as prioridades
		mt1.thrd.setPriority(Thread.NORM_PRIORITY + 2); // Alta prioridade
		mt2.thrd.setPriority(Thread.NORM_PRIORITY - 2); // Baixa prioridade
		
		// Inicia as threads
		mt1.thrd.start();
		mt2.thrd.start();
		
		try {
			mt1.thrd.join();
			mt2.thrd.join();
			
		} catch (InterruptedException exc) { IO.println("Main thread interrupted."); }
		
		// Exibe os resultados
		IO.println("\nHigh priority thread counted to " + mt1.count);
		IO.println("Low priority thread counted to " + mt2.count);
	}
}