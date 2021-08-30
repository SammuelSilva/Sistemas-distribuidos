package pub;

import java.util.Set;
import java.util.SortedSet;
import java.util.List;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.PubSubCommand;

public class NotifyCommand implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, List<String> subscribers, Address backup) {
		Message response = new MessageImpl();
			
		//System.out.println("Number of Log itens of an Observer " + m.getBrokerId() + " : " + log.size());

		synchronized(log){
			
			if(m.getContent().split(" ")[0].equals("REL")){
				String resource = m.getContent().split(" ")[1];
				String[] clientInfo = m.getContent().split(" ")[3].split(":");
				this.dirt(log, resource, clientInfo[0], clientInfo[1]);
				// System.out.println("Release notified" + m.getContent());
				response.setContent("Release notified: " + m.getContent());
			}else{
				// System.out.println("MEssage notified" + m.getContent());
				response.setContent("Message notified: " + m.getContent());
			}

			log.add(m);

			//System.out.println("*******************************************************");			
			//System.out.println("Adicionou no log: " + m.getContent());
			//log.stream().forEach(l -> System.out.print(l + " "));
			//System.out.println();
			//System.out.println("*******************************************************");			

			
			response.setType("notify_ack");
			log.notifyAll();
		}

		return response;

	}

	public void dirt(SortedSet<Message> log_set, String resource , String clientAddr, String clientPort){
		/**
		 * function to localize log to be released and 
		 */
		for(Message lg: log_set){
			if(lg.getType().equals("notify")){
				if(lg.getContent().split("->")[0].split(" ")[0].equals("ACQ") && lg.getContent().split("->")[0].split(" ")[1].equals(resource)){
					lg.setContent("FIN " + resource + " -> " + clientAddr + ":" + clientPort);
					lg.setType("fin");
					break;
				}
			}
		}

	}
}

