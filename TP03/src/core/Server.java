package core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

import utils.Tuple;

//this server represents the producer in a producer/consumer strategy
//it receives a client socket and inserts it into a resource
public class Server {
	protected PubSubConsumer<Socket> consumer;
	protected GenericResource<Socket> resource;
	
	protected SyncConsumer<Tuple<Socket, Message>> syncConsumer;
	protected GenericResource<Tuple<Socket, Message>> syncResource;

	protected CommandConsumer<Tuple<Socket, Message>> commandConsumer;
	protected GenericResource<Tuple<Socket, Message>> commandResource;
	
	protected int port;
	protected ServerSocket serverSocket;
	protected Boolean primary;
	protected Address address;

	public Server(int port, Boolean primary, Address address) {
		this.port = port;
		this.primary = primary;
		this.address = address;

		resource = new GenericResource<Socket>();
		syncResource = new GenericResource<Tuple<Socket, Message>>();
		commandResource = new GenericResource<Tuple<Socket, Message>>();
	}
	
	public Server(int port, boolean isPrimary){
		this(port, isPrimary, new Address(null, 0));
	}
	
	public Server(int port){
		this(port, true, new Address(null, 0));
	}

	public void begin(){
		try{
			
			//just one consumer to guarantee a single
			//log write mechanism
			consumer = new PubSubConsumer<Socket>(resource, commandResource, syncResource);
			
			commandConsumer = new CommandConsumer<>(commandResource, primary, address,
						 consumer.getMessages(), consumer.getSubscribers());

			if(!primary){
				syncConsumer = new SyncConsumer<Tuple<Socket, Message>>(syncResource, address,
						 consumer.getMessages(), consumer.getSubscribers());		
				
				syncConsumer.start();
			}
			
			commandConsumer.start();
			consumer.start();
			
			openServerSocket();
			
			//start listening 
			listen();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	protected void listen(){
		
		 		
        while(! resource.isStopped()){
            
            try {
            	Socket clientSocket = this.serverSocket.accept();
                
            	resource.putRegister(clientSocket);
            } catch (IOException e) {
                if(resource.isStopped()) {
                    System.out.println("[ Server ] Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                    "Error accepting connection", e);
            } 
            
            
        }
        System.out.println("[ Server ] Stopped: " + port) ;
        
	}	
    
    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            System.out.println("[ Server ] Listening on port: " + this.port);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + port, e);
        }
    }
    
    public void stop(){
    	resource.stopServer();
		syncResource.stopServer();
		commandResource.stopServer();
    	listen();
    	
    	consumer.stopConsumer();
		commandConsumer.stopConsumer();

    	resource.setFinished();
		syncResource.setFinished();
		commandResource.setFinished();
    	//consumer.interrupt();
    	try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	//System.out.println("verifying consumer.... " + consumer.isInterrupted());
    }
    
    public Set<Message> getLogMessages(){
    	try{
    		return ((PubSubConsumer<Socket>)consumer).getMessages();
    	}catch (Exception e){
    		return null;
    	}
    }
        
}
