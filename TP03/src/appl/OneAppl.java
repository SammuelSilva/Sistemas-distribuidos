/*
* Aplicação possui tres clientes, cada um usando uma porta (diferente da 8080)
* 
*/

package appl;

import java.util.SortedSet;
import java.util.TreeSet;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import core.Message;
import core.MessageComparator;

public class OneAppl {

	public static void main(String[] args) throws Exception {
		int brokerPort;
		String brokerAddr;
		String clientAddress;
		int clientPort;
		if (args.length != 0) {
			brokerPort = Integer.parseInt(args[0]);
			brokerAddr = args[1];
			clientAddress = args[2];
			clientPort = Integer.parseInt(args[3]);
		} else {
			brokerPort = 8080;
			brokerAddr = "localhost";
			clientAddress = "localhost";
			clientPort = 8082;
		}
		// TODO Auto-generated method stub
		new OneAppl(true, brokerPort, brokerAddr, clientAddress, clientPort);
	}
	
	public OneAppl(){
		PubSubClient client = new PubSubClient();
		client.startConsole();
	}
	
	public OneAppl(boolean flag, int brokerPort, String brokerAddr, String clientAddress, int clientPort) throws Exception{
		// Criação dos clientes
		PubSubClient client = new PubSubClient(clientAddress, clientPort);

		// Os clientes se inscrevem ao broker 8080 (demonstram interesse)
		client.subscribe("Test", brokerAddr, brokerPort);
		
		String[] resources = {"varA", "varB", "varC", "varD"};
		try {
			for(int i = 0; i < 10; i++){
				Random rand = new Random();	
				int randomNum = rand.nextInt(resources.length);		
				String rr = resources[randomNum];
				
				String resource = client.acquire(rr, "Test");
				client.useResource(resource);
				client.release(rr, "Test");
			}

			SortedSet<Message> logClient = new TreeSet<Message>(new MessageComparator());
			logClient.addAll(client.getLogMessages());

			Iterator<Message> it = logClient.iterator();

			System.out.print("\nLog Client itens: \n");
			while(it.hasNext()){
				Message aux = it.next();
				System.out.print("[ Content ]: " + aux.getContent() + " [LogID]: " + aux.getLogId() + " [type]: " + aux.getType() + "\n");
			}
			System.out.println();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally{
			client.unsubscribe("Test");
			client.stopPubSubClient();
		}		
	}
}
