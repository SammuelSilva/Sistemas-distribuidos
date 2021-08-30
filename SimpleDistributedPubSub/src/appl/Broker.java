package appl;

import core.Address;
import core.Message;
import core.MessageImpl;
import core.Server;
import core.client.Client;

import java.util.Scanner;

public class Broker {

	public static void main(String[] args) {
		String id = "localhost";
		if (args.length == 1){
			System.out.println("[ Broker ] Missing configuration data.");
			id = args[0];
		}
		new Broker(id);
	}
	
	public Broker(String ip){		
		/*
		* @Var: port -> Valor lido da entrada que define qual porta o seridor irá se conectar
		* @Var: s -> Servidor criado na usando port
		* @Var: brokerThread -> Cria uma thread para o servidor s
				É Ativado com o comando start()
		*/
		Scanner reader = new Scanner(System.in);  // Reading from System.in
		System.out.print("[ Sys ] Enter the Broker port number: ");
		int port = reader.nextInt(); // Scans the next token of the input as an int.
		Address brokerAddress = new Address(ip, port);
		
		System.out.print("[ Sys ] Primary Broker? (Y,N)");
		Boolean answer = reader.next().toLowerCase().equals("y");

		ThreadWrapper brokerThread;
		Server s;
		Boolean Bck_Activated = false;

		// Se o usuário responder sim, o servidor é criado como Primary Broker
		if (answer){
			s = new Server(port, answer);
			brokerThread = new ThreadWrapper(s);
			brokerThread.start();
		}else{ // Se o usuário responder não, o servidor é criado como Backup Broker
			System.out.print("[ Sys ] Enter the Primary Broker IP: ");
			String primaryIP = reader.next();
			System.out.print("[ Sys ] Enter the Primary Broker port: ");
			int primaryPort = reader.nextInt();
			
			s = new Server(port, answer, new Address(primaryIP, primaryPort));
			brokerThread = new ThreadWrapper(s);
			brokerThread.start();

			Message msgB = new MessageImpl();
			msgB.setType("Bck_Sub");
			msgB.setBrokerId(port);
			msgB.setContent(brokerAddress.toString());

			Message sendRecv_asw = null;

			try {
				System.out.println("[ Sys ] PORT: " + primaryIP + ":" + primaryPort);
				Client sub = new Client(primaryIP, primaryPort);
				sendRecv_asw = sub.sendReceive(msgB);
				//System.out.println("[ Sys ] Type:" + sendRecv_asw.getType());
			} catch (Exception e) {
				Bck_Activated = false;
			}

			if (sendRecv_asw != null && sendRecv_asw.getType().equals("Bck_Sub_Ack")){
				Bck_Activated = true;
			}
		}

		// Se o backup foi ativado ou o broker corresponde ao primario
		if (Bck_Activated || answer){
			System.out.println("[ Sys ] Broker is now running. Want to shutdown? (Y | N)");
			Boolean shutdown = reader.next().toLowerCase().equals("y");
			
			if (shutdown){
				System.out.println("[ Sys ] Broker stopped...");
				s.stop();
				brokerThread.interrupt();
			}
		}else{
			System.out.println("[ Sys ] Backup broker not valid or broker is not primary.");
			s.stop();
			brokerThread.interrupt();
		}

		//once finished
		reader.close();
	}
	
	class ThreadWrapper extends Thread{
		/*
		* @Var: s -> Servidor usado na Thread
		*/
		Server s;
		public ThreadWrapper(Server s){
			/*
			 * @param: s -> Servidor que será usado na thread 
			 */
			this.s = s;
		}

		public void run(){
			/*
			 * Coloca a Thread em execução 
			 */
			s.begin();
		}
	}

}
