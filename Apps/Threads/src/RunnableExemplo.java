
class RunnableExemplo implements Runnable 
{
	public void run() {
		IO.println("o runnable on ");
	}
	void main()
	{
		Thread t=new Thread(new RunnableExemplo());
		t.start();
	}
}
