/*
* Aplicação possui tres clientes, cada um usando uma porta (diferente da 8080)
* 
*/

package appl;

import java.util.Iterator;
//import java.util.List;
import java.util.Set;

import core.Message;

public class OneAppl {

	public static void main(String[] args) {
		int brokerPort = Integer.parseInt(args[0]);
		String brokerAddr = args[1];
		String clientAddress = args[2];
		int clientPort = Integer.parseInt(args[3]);
		// TODO Auto-generated method stub
		new OneAppl(true, brokerPort, brokerAddr, clientAddress, clientPort);
	}
	
	public OneAppl(){
		PubSubClient client = new PubSubClient();
		client.startConsole();
	}
	
	public OneAppl(boolean flag, int brokerPort, String brokerAddr, String clientAddress, int clientPort){
		// Criação dos clientes
		PubSubClient client = new PubSubClient(clientAddress, clientPort);

		// Os clientes se inscrevem ao broker 8080 (demonstram interesse)
		client.subscribe(brokerAddr, brokerPort);		
		try {
			//client.publish("biscoito", "localhost", 8080);
			String cred = client.acquire("door", brokerAddr, brokerPort);
			client.useResource(cred, "door");
			client.release("door", brokerAddr, brokerPort);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Set<Message> logClient = client.getLogMessages();
		
		Iterator<Message> it = logClient.iterator();
		System.out.print("Log Client itens: \n");
		while(it.hasNext()){
			Message aux = it.next();
			System.out.print("[Content]: " + aux.getContent() + " [LogID]: " + aux.getLogId() + " [type]: " + aux.getType() + "\n");
		}
		System.out.println();
				
		client.unsubscribe("localhost", 8080);


		client.stopPubSubClient();

	}
}
