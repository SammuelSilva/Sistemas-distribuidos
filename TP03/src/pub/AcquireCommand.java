package pub;

import java.util.Set;
import java.util.SortedSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

public class AcquireCommand implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, List<String> subscribers, Address backup) {
		
        Message response = new MessageImpl();
		int logId = m.getLogId();
		logId++;
		
		response.setLogId(logId);
		m.setLogId(logId);
		
		log.add(m);
		
		if(!backup.empty()){
			try {
				Client client = new Client(backup.getIp(), backup.getPort());
				Message backupMessage = new MessageImpl();
				backupMessage.setLogId(m.getLogId());
				backupMessage.setType("msgSync");
				backupMessage.setContent(m.getType() + "=>" + m.getContent());
				backupMessage.setBrokerId(m.getBrokerId());
				client.sendReceive(backupMessage);
			} catch (Exception e) {
				System.out.println("[ Broker ] Removing Backup");
				backup.setIp(null);			
			}
		}

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

    	String[] tokens = msg.getContent().split("->");
        String resource = tokens[0].split(" ")[1];
        String port = tokens[1].split(":")[1];

		response.setContent(resource + "_credentials:" + port);

		response.setType("acq_ack");
		
		return response;

	}

}
