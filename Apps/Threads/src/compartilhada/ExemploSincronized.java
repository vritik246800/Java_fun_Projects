package compartilhada;

import java.util.ArrayList;
import java.util.List;

public class ExemploSincronized {
	
	private  static int varCompartilhada=0;
	private static final Integer QUANTIDADE=10000;
	private static final List<Integer> VALORES = new ArrayList<>();
	
	void main() {
		
		Thread t1=new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i=0;i<QUANTIDADE; i++) {
					syync();
				}
				
			}
		});
	
	
			
		Thread t2=new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i=0;i<QUANTIDADE; i++) {
					syync();
				}
				
			}
		});
	
	
	
		
		Thread t3=new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i=0;i<QUANTIDADE; i++) {
					syync();
				}
				
			}
		});
		
		t1.start();
		t2.start();
		t3.start();
		
		try {
			t1.join();
			t2.join();
			t3.join();
		} catch(InterruptedException ex) {
			ex.printStackTrace();
		}
		
		int soma=0;
		
		for(Integer valor : VALORES) {
			soma+=valor;
		}
		IO.println("Soma: " + soma);
	}
	
	public static synchronized void syync() {
		VALORES.add(++varCompartilhada);
	}
	
}
