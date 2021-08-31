package core;


import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.SortedSet;

import utils.Tuple;


//the useful socket consumer
public class SyncConsumer<S extends Tuple<Socket, Message>> extends GenericConsumer<S>{

	private SortedSet<Message> log;
	private List<String> subscribers;

    public SyncConsumer(GenericResource<S> re, Address primary, SortedSet<Message> log, List<String> subscribers) {		
		super(re);
		this.log = log;	
		this.subscribers = subscribers;
	}

    @Override
    protected void doSomething(S t) {
        try{
			
			Socket str = t.getKey();
			Message msg = t.getValue();

			Message response = new MessageImpl();

			response = commands.get(msg.getType()).execute(msg, log, subscribers, null);

			ObjectOutputStream out = new ObjectOutputStream(str.getOutputStream());
			out.writeObject(response);
			out.flush();
			out.close();						
			str.close();
				
		}catch (Exception e){
			e.printStackTrace();
			
		}
				
	}	
	
	public SortedSet<Message> getMessages(){
		return log;
	}
    
}
