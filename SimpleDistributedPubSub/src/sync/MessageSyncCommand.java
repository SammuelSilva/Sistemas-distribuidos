package sync;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.PubSubCommand;
import core.client.Client;

public class MessageSyncCommand implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, List<String> subscribers, Address backup) {
		
		String[] content = m.getContent().split("=>");
		m.setType(content[0]);
		m.setContent(content[1]);

		log.add(m);

		if (m.getType().equals("sub"))
			subscribers.add(m.getContent());
		
		if (m.getType().equals("unsub")){
			subscribers.remove(m.getContent());
		}
		
		if (m.getType().equals("rel")) {
			//System.out.println("----------------------===============================-----------------------");
			//System.out.println("Received a release message: " + m.getContent());
			//System.out.println("----------------------===============================-----------------------");

			String resource = m.getContent().split(" ")[1];
			String[] clientInfo = m.getContent().split(" ")[3].split(":");

			for(Message lg: log){
				if(lg.getType().equals("acq")){
					if(lg.getContent().split("->")[0].split(" ")[0].equals("ACQ") && lg.getContent().split("->")[0].split(" ")[1].equals(resource)){
						lg.setContent("FIN " + resource + " -> " + clientInfo[0] + ":" + clientInfo[1]);
						lg.setType("fin");
						break;
					}
				}
			}

		}

        Message response = new MessageImpl();

		response.setContent("Message synchronized: " + m.getContent());
		response.setType("sync_ack");
		
		System.out.println("[ Sys-Sync ] Message synchronized: (" + m.getType() + ") " +  m.getContent());

		return response;

	}

}
