package compartilhada;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ExemploAssincrono2 {

	private static final AtomicInteger varCompartilhada = new AtomicInteger(0);
	private static final Integer QUANTIDADE = 10000;
	private static final List<Integer> VALORES = new CopyOnWriteArrayList<>();

	void main() {

		Runnable tarefa = () -> {
			for (int i = 0; i < QUANTIDADE; i++) {
				VALORES.add(varCompartilhada.incrementAndGet());
			}
		};

		Thread t1 = new Thread(tarefa);
		Thread t2 = new Thread(tarefa);
		Thread t3 = new Thread(tarefa);

		t1.start();
		t2.start();
		t3.start();

		try {
			t1.join();
			t2.join();
			t3.join();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		long soma = 0;
		for (Integer valor : VALORES) {
			soma += valor;
		}
		IO.println("Soma: " + soma);
	}
}