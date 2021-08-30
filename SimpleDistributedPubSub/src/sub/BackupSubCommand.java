package sub;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

/**
 * Comando executado pelo broker primário quando um broker backup tenta
 * se subscrever.
 */
public class BackupSubCommand implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, List<String> subscribers, Address backup) {
		
		Message response = new MessageImpl();
				
		if(! backup.empty()) {
			response.setContent("Backup already exists");
			response.setType("backupSub_error");
		}
		else{
			int logId = m.getLogId();
			logId++;
			
			response.setLogId(logId);
			m.setLogId(logId);
			
			log.add(m);

			String[] ipAndPort = m.getContent().split(":");
			backup.setIp(ipAndPort[0]);
			backup.setPort(Integer.parseInt(ipAndPort[1]));
			
			System.out.println("[ Broker ] Backup not found! Start to synchronize.");
			response.setContent("Backup added: " + m.getContent());

			//start a client to send all existing log messages
			//to the backup
			if(!log.isEmpty()){
				for(Message msg : log){
					try {
						Client client = new Client(backup.getIp(), backup.getPort());
						Message aux = new MessageImpl();					
						aux.setType("msgSync");
						aux.setContent(msg.getType() + "=>" + msg.getContent());
						aux.setLogId(msg.getLogId());
						aux.setBrokerId(msg.getBrokerId());
						client.sendReceive(aux);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("[ Broker ] BackupSubCommand: Bck_Sub_Ack");
			response.setType("Bck_Sub_Ack");
		
		}
		
		System.out.println("[ Broker ] BackupSubCommand: " + response.getContent());
		return response;

	}

}
