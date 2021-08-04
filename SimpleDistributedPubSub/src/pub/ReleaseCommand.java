package pub;

//Simport java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;

import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

public class ReleaseCommand implements PubSubCommand{
	/**
	 * Responsible for sending the release message to the client.
	 * It also sends the release message to all the clients that are subscribed to the topic.
	 * Change the log message to reflect the release of the resource.
	 */
	@Override
	public Message execute(Message m, SortedSet<Message> log, Set<String> subscribers) {

		String resource = m.getContent().split(" ")[1];
		String[] clientInfo = m.getContent().split(" ")[3].split(":");
		this.dirt(log, resource, clientInfo[0], clientInfo[1]);

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

		response.setContent("Released: " + resource);
        response.setType("release_ack");

		return response;
	}
	

	public void dirt(SortedSet<Message> log_set, String resource , String clientAddr, String clientPort){
		/**
		 * function to localize log to be released and 
		 */
		for(Message lg: log_set){
			
			if(lg.getType().equals("acq")){
				if(lg.getContent().split("->")[0].split(" ")[0].equals("ACQ") && lg.getContent().split("->")[0].split(" ")[1].equals(resource)){
					lg.setContent("FIN " + resource + " -> " + clientAddr + ":" + clientPort);
					lg.setType("fin");
					break;
				}
			}
		}

	}

}
