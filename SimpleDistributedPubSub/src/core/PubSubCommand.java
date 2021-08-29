package core;

import java.util.List;
import java.util.SortedSet;

public interface PubSubCommand {
	
	public Message execute(Message m, SortedSet<Message> log, List<String>subscribers, Address backup);

}
