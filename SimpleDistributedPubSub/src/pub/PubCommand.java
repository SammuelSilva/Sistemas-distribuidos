package pub;

import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

public class PubCommand implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, List<String> subscribers, Address backup) {
		Message response = new MessageImpl();
		int logId = m.getLogId();
		logId++;
		
		response.setLogId(logId);
		m.setLogId(logId);
		
		log.add(m);
		
		Message msg = new MessageImpl();
		msg.setContent(m.getContent());
		msg.setLogId(logId);
		msg.setType("notify");
		
		
		CopyOnWriteArrayList<String> subscribersCopy = new CopyOnWriteArrayList<String>();
		subscribersCopy.addAll(subscribers);
		try{	
			for(String aux:subscribersCopy){
				String[] ipAndPort = aux.split(":");
				Client client = new Client(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
				msg.setBrokerId(m.getBrokerId());
				Message cMsg = client.sendReceive(msg);
				if(cMsg == null) subscribers.remove(aux);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		response.setContent("PUB(" + m.getContent() + ")");
		response.setType("pub_ack");
		
		return response;

	}

}
