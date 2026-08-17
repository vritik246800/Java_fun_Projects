package exemplo_multi;

class Tarefa extends Thread {
	private final long VALORINICIAL, VALORFINAL;
	private long total=0;
	
	Tarefa(int valorIncial, int valorFinal){
		this.VALORINICIAL=valorIncial;
		this.VALORFINAL=valorFinal;
	}
	
	long getTotal() {
		return total;
	}
	
	
	public void run() {
		for(long i=VALORINICIAL ; i<=VALORFINAL ; i++) {
			total+=i;
		}
	}
}