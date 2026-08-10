
class ThreadExamplo extends Thread {

	public void run()
	{
		IO.println("Resultado: "+calc());
	 	IO.println("O thread ja terminou !");
	}
	
	int calc()
	{
		final int V1=8,V2=15,V3=6,V4=2;
		
		return (V1+V2+V3)*V4;
	}

	void main()
	{
		ThreadExamplo mt=new ThreadExamplo();
		mt.start();
	}
}
