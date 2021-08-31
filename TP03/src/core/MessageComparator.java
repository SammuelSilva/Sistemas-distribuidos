package core;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message>{

	@Override
	public int compare(Message o1, Message o2) {
		//System.out.println("Comparando mensagens: " + o1.getLogId() + " e " + o2.getLogId());
		int hash1 = (String.valueOf(o1.getLogId()) + String.valueOf(o1.getBrokerId())).hashCode(); 
		int hash2 = (String.valueOf(o2.getLogId()) + String.valueOf(o2.getBrokerId())).hashCode();
		
		if(hash1>hash2) return 1;
		else if (hash1<hash2) return -1;
		
		//otherwise
		return 0;
	}

}
