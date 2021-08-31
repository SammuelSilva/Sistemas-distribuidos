package core;


//import java.io.IOException;
//import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import utils.Tuple;

//the useful socket consumer
public class PubSubConsumer<S extends Socket> extends GenericConsumer<S>{
	
	//private int uniqueLogId;
	private SortedSet<Message> log;
	private List<String> subscribers;
	
	private GenericResource<Tuple<Socket, Message>> syncResource;
	private GenericResource<Tuple<Socket, Message>> primaryResource;

	public PubSubConsumer(GenericResource<S> re, GenericResource<Tuple<Socket, Message>> primary,  GenericResource<Tuple<Socket, Message>> sync) {		
		super(re);
		this.log = new TreeSet<Message>(new MessageComparator());
		this.syncResource = sync;
        this.primaryResource = primary;

		this.subscribers = new ArrayList<String>();
	}
	
	
	@Override
	protected void doSomething(S str) {
		try{
			// TODO Auto-generated method stub
			ObjectInputStream in = new ObjectInputStream(str.getInputStream());
			
			Message msg = (Message) in.readObject();
			
			if (msg.getType().toLowerCase().contains("sync")) 
				syncResource.putRegister(new Tuple<Socket, Message>(str, msg));
			else 
				primaryResource.putRegister(new Tuple<Socket, Message>(str, msg)); 

		}catch (Exception e){
		 	e.printStackTrace();	
		}
				
	}	
	
	@Override
    public SortedSet<Message> getMessages() {
        return log;
    }

    public void setSyncResource(GenericResource<Tuple<Socket, Message>> syncResource){
		this.syncResource = syncResource;
	}

    public void setPrimaryResource(GenericResource<Tuple<Socket, Message>> primaryResource){
		this.primaryResource = primaryResource;
	}

    public List<String> getSubscribers() {
		return subscribers;
	}
}
