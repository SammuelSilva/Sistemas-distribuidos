package sub;

import java.util.Set;
import java.util.SortedSet;

import core.Message;
import core.MessageImpl;
import core.PubSubCommand;

public class SubCommandOne implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, Set<String> subscribers) {
				
		Message response = new MessageImpl();
		
		if(m.getType().equals("sub")){
				
			if(subscribers.contains(m.getContent())){
				response.setContent("subscriber exists: " + m.getContent());
				response.setLogId(m.getLogId());
			}else{
				int logId = m.getLogId();
				logId++;
				
				response.setLogId(logId);
				m.setLogId(logId);
				
				subscribers.add(m.getContent());
				log.add(m);
				
				response.setContent("Subscriber added: " + m.getContent());
				
			}
		}
		

		if(m.getType().equals("unsub")){
				
			if(!subscribers.contains(m.getContent())){
				response.setContent("subscriber does not exist: " + m.getContent());
				response.setLogId(m.getLogId());
			}else{
				int logId = m.getLogId();
				logId++;
				
				response.setLogId(logId);
				m.setLogId(logId);
				
				subscribers.remove(m.getContent());
								
				response.setContent("Subscriber removed: " + m.getContent());
				
			}
		}
		
		response.setType("sub_ack");
		
		return response;

	}

}
