package sub;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

public class SubCommandTwo implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, Set<String> subscribers) {
		
		Message response = new MessageImpl();

		if(m.getType().equals("sub")){
				
			if(subscribers.contains(m.getContent()))
				response.setContent("E_SUB(" + m.getContent() + ")");
			else{
				int logId = m.getLogId();
				logId++;
				
				response.setLogId(logId);
				m.setLogId(logId);
				
				subscribers.add(m.getContent());
				log.add(m);
				
				response.setContent("ADD_SUB(" + m.getContent() + ")");
				
				//start many clients to send all existing log messages
				//for the subscribed user
				if(!log.isEmpty()){
					Iterator<Message> it = log.iterator();
					String[] ipAndPort = m.getContent().split(":");
					while(it.hasNext()){
						Client client = new Client(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
						Message msg = it.next();
						Message aux = new MessageImpl();
						aux.setType("notify");
						aux.setContent(msg.getContent());
						aux.setLogId(msg.getLogId());
						aux.setBrokerId(m.getBrokerId());
						Message cMsg = client.sendReceive(aux);
						if(cMsg == null) {
							subscribers.remove(m.getContent());
							break;
						}
					}
				}
			
			}
			response.setType("sub_ack");
		}
		
		if(m.getType().equals("unsub")){
			
			if(!subscribers.contains(m.getContent()))
				response.setContent("DNE_SUB(" + m.getContent() + ")");
			else{
				int logId = m.getLogId();
				logId++;
				
				response.setLogId(logId);
				m.setLogId(logId);
				
				subscribers.remove(m.getContent());
								
				response.setContent("RM_SUB(" + m.getContent() + ")");
					
			}
			response.setType("unsub_ack");
		}
		
		
		return response;

	}

}

