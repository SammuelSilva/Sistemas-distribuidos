package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.Server;
import core.client.Client;

import utils.Tuple;


public class PubSubClient {
	
	private Server observer;
	private ThreadWrapper clientThread;
	
	private Address clientAddress;
	private int clientPort;
	
	// brokername -> Tupla de endereços (primario e secundário)
	private Map<String, Tuple<Address, Address>> brokerAddresses;

	public PubSubClient(){
		//this constructor must be called only when the method
		//startConsole is used
		//otherwise the other constructor must be called
		this.brokerAddresses = new HashMap<String, Tuple<Address, Address>>(); // garante que posso ter multiplos brokers
	}
	
	public PubSubClient(String clientAddress, int clientPort){
		this();

		this.clientAddress = new Address(clientAddress, clientPort);

		observer = new Server(clientPort);
		clientThread = new ThreadWrapper(observer);
		clientThread.start();
	}
	
	public void subscribe(String brokerName, String brokerAddress, int brokerPort){
		if (this.brokerAddresses.containsKey(brokerName)){
			throw new IllegalStateException("[ Sys ] broker called " + brokerName + " already exists.");
		}
		
		Message msgBroker = new MessageImpl();
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setType("sub");
		msgBroker.setContent(this.clientAddress.toString());
		
		try{
			Client subscriber = new Client(brokerAddress, brokerPort);
			System.out.println("[ Sys ] Subscribing to broker " + brokerPort + " at " + msgBroker.getContent());
			Message response = subscriber.sendReceive(msgBroker);
			//System.out.println("[ Debug ] " + response.getContent().isEmpty());
			if (response.getType().equals("sub_ack")){
				String backup = response.getContent();
				this.brokerAddresses.put(brokerName, new Tuple<>(new Address(brokerAddress, brokerPort), 
											backup.isEmpty() ? null : new Address(backup.split(":")[0], Integer.parseInt(backup.split(":")[1]))));
			}
		}catch (IOException e){
			throw new IllegalStateException("There is no broker at " + brokerAddress + ":" + brokerPort);
		}
	}

	public String acquire(String resource, String brokerName) throws Exception, InterruptedException{	
		if (!this.brokerAddresses.containsKey(brokerName)){
			throw new IllegalStateException("[ Sys ] broker called " + brokerName + " doesn't exists.");
		}

		Address main;

		while((main = this.brokerAddresses.get(brokerName).getKey()) != null){

			Message msgAcq = new MessageImpl();
			msgAcq.setBrokerId(main.getPort());
			msgAcq.setType("acq");
			msgAcq.setContent("ACQ " + resource + " -> " + clientAddress + ":" + clientPort);

			try{
				Client acquired = new Client(main.getIp(), main.getPort());
				Message r_msg = acquired.sendReceive(msgAcq);

				if (r_msg.getType().equals("acq_ack")){
					int id = r_msg.getLogId();
					while(true){
						Set<Message> logs = this.observer.getLogMessages();
						synchronized(logs) {
							Iterator<Message> log = logs.iterator();
							boolean found = false;
							Message firstAquired = null;

							//Adicionou no log: ACQ var A -> localhost:8081:0
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
										System.out.println("[ Sys ] Waiting...");
										logs.wait();
										System.out.println("[ Sys ] Woke up!");
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
			}catch (IOException e){
				System.out.println("[ Client ] Failed to execute command, server down. Using the Backup.");
				recover(brokerName);
			}
		}

		throw new IllegalStateException("There is no broker available.");
	}

	public boolean release(String resource, String brokerName) throws IOException{
		if (!this.brokerAddresses.containsKey(brokerName)){
			throw new IllegalStateException("[ Sys ] broker called " + brokerName + " doesn't exists.");
		}

		Address main;

		while((main = this.brokerAddresses.get(brokerName).getKey()) != null){
			Message msgRel = new MessageImpl();
			msgRel.setBrokerId(main.getPort());
			msgRel.setType("rel");
			msgRel.setContent("REL " + resource + " -> " + clientAddress + ":" + clientPort);
		
			try{
				Client released = new Client(main.getIp(), main.getPort());
				released.sendReceive(msgRel);
				return true;
			}catch (IOException e){
				System.out.println("[ Client ] Failed to execute command, server down. Using the Backup.");
				recover(brokerName);
			}
		}
		throw new IllegalStateException("There is no broker available.");
	}

	public void publish(String message, String brokerAddress, int brokerPort){
		Message msgPub = new MessageImpl();
		msgPub.setBrokerId(brokerPort);
		msgPub.setType("pub");
		msgPub.setContent(message);
		
		//Client publisher = new Client(brokerAddress, brokerPort);
		//publisher.sendReceive(msgPub);
		
	}
	
	public void unsubscribe(String brokerName) throws IOException{
		if (!this.brokerAddresses.containsKey(brokerName)){
			throw new IllegalStateException("[ Sys ] broker called " + brokerName + " doesn't exists.");
		}

		Address main;

		while((main = this.brokerAddresses.get(brokerName).getKey()) != null){
			Message msgBroker = new MessageImpl();
			msgBroker.setBrokerId(main.getPort());
			msgBroker.setType("unsub");
			msgBroker.setContent(this.clientAddress.toString());
			try{
				Client subscriber = new Client(main.getIp(), main.getPort());
				subscriber.sendReceive(msgBroker);
				return;
			}catch (IOException e){
				System.out.println("[ Client ] Failed to execute command, server down. Using the Backup.");
				recover(brokerName);
			}
		}
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
		System.out.println(" [ Client ] Client stopped");
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
		try{
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
		}catch(Exception e){
			e.printStackTrace();
		}
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

	public void useResource(String cred) throws InterruptedException {
		//System.out.println(" [ Debug ] Using resource: "  +  cred);
		Thread.sleep(4000);
		//System.out.println(" [ Debug ] Finished resource usage: "  + cred);
	}	

		// manda o próximo broker da tupla virar primário, mesmo sem ele saber qual é o primário.
	private void recover(String brokerName) throws IOException {
		// muda o primário na tupla
		Tuple<Address, Address> tuple = this.brokerAddresses.get(brokerName);

		tuple.setKey(tuple.getValue());
		tuple.setValue(null);
	
		Address mainBroker = this.brokerAddresses.get(brokerName).getKey();
	
		if(mainBroker != null) {
						
			Message msgTurn = new MessageImpl();
			msgTurn.setBrokerId(mainBroker.getPort());
			msgTurn.setType("recover");
				
			Client pub_acq = new Client(mainBroker.getIp(), mainBroker.getPort());
	
			Message response = pub_acq.sendReceive(msgTurn);
	
			if (response.getType().equals("recover_ack"))
				return;
		}
		throw new IllegalStateException("Can't convert secundary to primary broker.");
	}
}
