package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import core.Message;
import core.MessageImpl;
import core.Server;
import core.client.Client;

public class PubSubClient {
	
	private Server observer;
	private ThreadWrapper clientThread;
	
	private String clientAddress;
	private int clientPort;
	
	public PubSubClient(){
		//this constructor must be called only when the method
		//startConsole is used
		//otherwise the other constructor must be called
	}
	
	public PubSubClient(String clientAddress, int clientPort){
		this.clientAddress = clientAddress;
		this.clientPort = clientPort;
		observer = new Server(clientPort);
		clientThread = new ThreadWrapper(observer);
		clientThread.start();
	}
	
	public void subscribe(String brokerAddress, int brokerPort){
					
		Message msgBroker = new MessageImpl();
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setType("sub");
		msgBroker.setContent(clientAddress+":"+clientPort);
		Client subscriber = new Client(brokerAddress, brokerPort);
		System.out.println(subscriber.sendReceive(msgBroker).getContent());
	}

	public String acquire(String resource, String brokerAddress, int brokerPort) throws Exception{
		Message msgAcq = new MessageImpl();
		msgAcq.setBrokerId(brokerPort);
		msgAcq.setType("acq");
		msgAcq.setContent("ACQ " + resource + " -> " + clientAddress + ":" + clientPort);

		Client acquired = new Client(brokerAddress, brokerPort);
		Message r_msg = acquired.sendReceive(msgAcq);

		if (r_msg.getType().equals("acq_ack")){
			int id = r_msg.getLogId();
			while(true){
				Set<Message> logs = this.observer.getLogMessages();
				synchronized(logs) {
					Iterator<Message> log = logs.iterator();
					boolean found = false;
					Message firstAquired = null;

					// Find the first message acquired
					while(!found && log.hasNext()){
						Message aux = log.next();
						if(aux.getContent().contains("ACQ") && aux.getContent().contains(resource)){
							firstAquired = aux;
							found = true;
						}
					}

					if(firstAquired != null){
						if(firstAquired.getLogId() != id){
							try {
								System.out.println("Waiting...");
								logs.wait();
								System.out.println("Woke up!");
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}else{
							return r_msg.getContent(); // credential acquired
						}
					}else{
						throw new Exception("No acquired found");
					}
				}
			}
		}
		throw new Exception("No credential acquired");
	}

	

	public void release(String resource, String brokerAddress, int brokerPort){
		Message msgRel = new MessageImpl();
		msgRel.setBrokerId(brokerPort);
		msgRel.setType("rel");
		msgRel.setContent("REL " + resource + " -> " + clientAddress + ":" + clientPort);
		
		Client released = new Client(brokerAddress, brokerPort);
		Message r_msg = released.sendReceive(msgRel);
	}

	public void publish(String message, String brokerAddress, int brokerPort){
		Message msgPub = new MessageImpl();
		msgPub.setBrokerId(brokerPort);
		msgPub.setType("pub");
		msgPub.setContent(message);
		
		Client publisher = new Client(brokerAddress, brokerPort);
		publisher.sendReceive(msgPub);
		
	}
	
	public void unsubscribe(String brokerAddress, int brokerPort){
		
		Message msgBroker = new MessageImpl();
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setType("unsub");
		msgBroker.setContent(clientAddress+":"+clientPort);
		Client subscriber = new Client(brokerAddress, brokerPort);
		subscriber.sendReceive(msgBroker);
	}

	public List<Message> getMessages(){
		CopyOnWriteArrayList<Message> logCopy = new CopyOnWriteArrayList<Message>();
		logCopy.addAll(observer.getLogMessages());
		
		return logCopy;
	}

	public Set<Message> getLogMessages(){
		return observer.getLogMessages();
	}

	public void stopPubSubClient(){
		System.out.println("Client stopped...");
		observer.stop();
		clientThread.interrupt();
	}
		
	public void startConsole(){
		Scanner reader = new Scanner(System.in);  // Reading from System.in
		System.out.print("Enter the client address (ex. localhost): ");
		String clientAddress = reader.next();
		System.out.print("Enter the client port (ex.8080): ");
		int clientPort = reader.nextInt();
		System.out.println("Now you need to inform the broker credentials...");
		System.out.print("Enter the broker address (ex. localhost): ");
		String brokerAddress = reader.next();
		System.out.print("Enter the broker port (ex.8080): ");
		int brokerPort = reader.nextInt();
		
		observer = new Server(clientPort);
		clientThread = new ThreadWrapper(observer);
		clientThread.start();
		
		Message msgBroker = new MessageImpl();
		msgBroker.setType("sub");
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setContent(clientAddress+":"+clientPort);
		Client subscriber = new Client(brokerAddress, brokerPort);
		subscriber.sendReceive(msgBroker);
		
		System.out.println("Do you want to subscribe for more brokers? (Y|N)");
		String resp = reader.next();
		
		if(resp.equals("Y")||resp.equals("y")){
			String message = "";
			Message msgSub = new MessageImpl();
			msgSub.setType("sub");
			msgSub.setContent(clientAddress+":"+clientPort);
			while(!message.equals("exit")){
				System.out.println("You must inform the broker credentials...");
				System.out.print("Enter the broker address (ex. localhost): ");
				brokerAddress = reader.next();
				System.out.print("Enter the broker port (ex.8080): ");
				brokerPort = reader.nextInt();
				subscriber = new Client(brokerAddress, brokerPort);
				msgSub.setBrokerId(brokerPort);
				subscriber.sendReceive(msgSub);
				System.out.println(" Write exit to finish...");
				message = reader.next();
			}
		}
		
		System.out.println("Do you want to publish messages? (Y|N)");
		resp = reader.next();
		if(resp.equals("Y")||resp.equals("y")){
			String message = "";			
			Message msgPub = new MessageImpl();
			msgPub.setType("pub");
			while(!message.equals("exit")){
				System.out.println("Enter a message (exit to finish submissions): ");
				message = reader.next();
				msgPub.setContent(message);
				
				System.out.println("You must inform the broker credentials...");
				System.out.print("Enter the broker address (ex. localhost): ");
				brokerAddress = reader.next();
				System.out.print("Enter the broker port (ex.8080): ");
				brokerPort = reader.nextInt();
				
				msgPub.setBrokerId(brokerPort);
				Client publisher = new Client(brokerAddress, brokerPort);
				publisher.sendReceive(msgPub);
				
				Set<Message> log = observer.getLogMessages();
				
				Iterator<Message> it = log.iterator();
				System.out.print("Log itens: ");
				while(it.hasNext()){
					Message aux = it.next();
					System.out.print(aux.getContent() + aux.getLogId() + " | ");
				}
				System.out.println();

			}
		}
		
		System.out.print("Shutdown the client (Y|N)?: ");
		resp = reader.next(); 
		if (resp.equals("Y") || resp.equals("y")){
			System.out.println("Client stopped...");
			observer.stop();
			clientThread.interrupt();
			
		}
		
		//once finished
		reader.close();
	}
	
	class ThreadWrapper extends Thread{
		Server s;
		public ThreadWrapper(Server s){
			this.s = s;
		}
		public void run(){
			s.begin();
		}
	}

	public void useResource(String cred, String resource) throws InterruptedException {
		System.out.println("Using resource: " + resource + ":" + cred);
		Thread.sleep(4000);
		System.out.println("Finished resource usage: " + resource + ":" + cred);
	}	

}
