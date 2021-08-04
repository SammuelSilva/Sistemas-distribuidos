package pub;

import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;

import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

public class AcquireCommand implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, Set<String> subscribers) {
		
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
		for(String aux:subscribersCopy){
			String[] ipAndPort = aux.split(":");
			Client client = new Client(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
			msg.setBrokerId(m.getBrokerId());
			Message cMsg = client.sendReceive(msg);
			if(cMsg == null) subscribers.remove(aux);
		}
		
    	String[] tokens = msg.getContent().split("->");
        String resource = tokens[0].split(" ")[1];
        String port = tokens[1].split(":")[1];

		response.setContent(resource + "_credentials:" + port);

		response.setType("acq_ack");
		
		return response;

	}

}
