package core;


import java.util.LinkedList;

public class GenericResource<S> {
	private LinkedList<S> registers;
	protected boolean finished, serverFinished;
		
	public GenericResource(){
		this.registers = new LinkedList<S>();
		this.finished = false;		
	}
		
	public synchronized void putRegister(S register){
		
		this.registers.addLast(register);
		wakeup();
	}
	
	protected void wakeup(){
		this.notify();
	}
							
	public synchronized S getRegister() throws Exception{
		if(!this.registers.isEmpty())
			return this.registers.removeFirst();
		else {
			if(finished==false)
				suspend();
			return null;		
		}
	}
	
	protected synchronized void suspend()throws Exception{
		wait();
	}
	
	public int getNumOfRegisters(){
		return this.registers.size();
	}
	
	public synchronized void setFinished(){
		this.finished = true;
		this.notifyAll();
	}
	
	public boolean isFinished(){
		return this.finished;
	}
	
		
	public boolean isStopped(){
		return serverFinished;
	}
	
	public synchronized void stopServer(){
		serverFinished = true;
		this.notifyAll();
	}
}
