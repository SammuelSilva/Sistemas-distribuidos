package sub;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.List;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

public class SubCommandTwo implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, List<String> subscribers, Address backup) {
		
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

				response.setContent(!backup.empty() ? backup.toString() : "");
				
				//start many clients to send all existing log messages
				//for the subscribed user
				if(!log.isEmpty()){
					Iterator<Message> it = log.iterator();
					String[] ipAndPort = m.getContent().split(":");
					while(it.hasNext()){
						try{
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
						}catch(IOException e){
							e.printStackTrace();
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
				
				if (! backup.empty()){
					try {
						Client client = new Client(backup.getIp(), backup.getPort());
						Message aux = new MessageImpl();					
						aux.setType("msgSync");
						aux.setContent(m.getType() + "=>" + m.getContent());
						aux.setLogId(m.getLogId());
						aux.setBrokerId(m.getBrokerId());
						client.sendReceive(aux);
					} catch (IOException e) {
						System.out.println("[ Broker ] Removing Backup");	
						backup.setIp(null);
					}
				}

				response.setContent("RM_SUB(" + m.getContent() + ")");
				log.add(m);
			}
			response.setType("unsub_ack");
		}
		
		
		return response;

	}

}

